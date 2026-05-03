package com.fuelmatch.nutrition.application.service;

import com.fuelmatch.nutrition.application.mapper.FoodMapper;
import com.fuelmatch.nutrition.application.port.FoodRepository;
import com.fuelmatch.nutrition.domain.model.Food;
import com.fuelmatch.nutrition.infrastructure.integration.openfoodfacts.client.OpenFoodFactsClient;
import com.fuelmatch.nutrition.infrastructure.integration.openfoodfacts.dto.OffProductResponse.OffProduct;
import com.fuelmatch.nutrition.infrastructure.integration.openfoodfacts.dto.OffSearchResponse;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Core service de busca de alimentos do FuelMatch.
 *
 * <h3>Fluxo de busca (fallback chain):</h3>
 * <pre>
 *  1. Cache Redis (hot path)
 *       ↓ MISS
 *  2. Banco local — Fuzzy Search via pg_trgm
 *       ↓ resultados insuficientes E query parece ser alimento comercial
 *  3. Open Food Facts API (síncrono, máx 5s)
 *       ↓ encontrado
 *  4. Retorna resultado + dispara background job para persistir no banco local
 * </pre>
 *
 * <p>O background job (step 4) é assíncrono e não bloqueia a resposta ao usuário.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FoodSearchService {

    private static final int LOCAL_MIN_RESULTS_THRESHOLD = 3;
    private static final int OFF_PAGE_SIZE = 10;

    private final FoodRepository foodRepository;
    private final OpenFoodFactsClient offClient;
    private final FoodMapper foodMapper;
    private final FoodPersistenceService persistenceService;

    // ── Search API ────────────────────────────────────────────────────────────

    /**
     * Busca principal de alimentos com fallback para Open Food Facts.
     *
     * @param query     termo de busca livre
     * @param source    filtro de fonte (null = todas)
     * @param tenantId  UUID do tenant para incluir alimentos customizados
     * @param pageable  paginação
     * @return página de alimentos, com resultados da OFF integrados se necessário
     */
    @Timed(value = "food.search", description = "Food search total latency")
    @Transactional(readOnly = true)
    public Page<Food> search(String query, Food.FoodSource source,
                              UUID tenantId, Pageable pageable) {

        log.debug("Iniciando busca: query='{}', source={}, page={}", query, source, pageable.getPageNumber());

        // ── Step 1: Banco local ───────────────────────────────────────────────
        Page<Food> localResults = foodRepository.fuzzySearch(query, source, tenantId, pageable);

        boolean resultsInsufficient = localResults.getTotalElements() < LOCAL_MIN_RESULTS_THRESHOLD;
        boolean isFirstPage = pageable.getPageNumber() == 0;
        boolean shouldCallOff = resultsInsufficient
                && isFirstPage
                && source != Food.FoodSource.TACO  // TACO não está na OFF
                && source != Food.FoodSource.CUSTOM;

        if (!shouldCallOff) {
            log.debug("Retornando {} resultados locais para '{}'",
                    localResults.getTotalElements(), query);
            return localResults;
        }

        // ── Step 2: Fallback para Open Food Facts ─────────────────────────────
        log.info("Resultados locais insuficientes ({}). Consultando Open Food Facts para '{}'",
                localResults.getTotalElements(), query);

        Optional<OffSearchResponse> offResponse = offClient.searchByName(
                query, pageable.getPageNumber() + 1, OFF_PAGE_SIZE);

        if (offResponse.isEmpty() || offResponse.get().getProducts() == null
                || offResponse.get().getProducts().isEmpty()) {
            log.debug("Open Food Facts sem resultados para '{}'", query);
            return localResults;
        }

        List<OffProduct> offProducts = offResponse.get().getProducts();

        // Mapeia produtos OFF → domínio
        List<Food> offFoods = offProducts.stream()
                .filter(this::hasMinimumNutritionalData)
                .map(foodMapper::fromOffProduct)
                .collect(Collectors.toList());

        if (offFoods.isEmpty()) {
            return localResults;
        }

        // ── Step 3: Merge local + OFF ─────────────────────────────────────────
        // Deduplica: remove da lista OFF alimentos que já existem localmente
        Set<String> localBarcodes = localResults.getContent().stream()
                .map(Food::getBarcode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Food> newOffFoods = offFoods.stream()
                .filter(f -> f.getBarcode() == null || !localBarcodes.contains(f.getBarcode()))
                .toList();

        // ── Step 4: Background job — persistência assíncrona ──────────────────
        if (!newOffFoods.isEmpty()) {
            scheduleAsyncPersistence(newOffFoods, query);
        }

        // Monta resultado combinado (local primeiro, OFF complementa)
        List<Food> combined = new ArrayList<>(localResults.getContent());
        combined.addAll(newOffFoods);

        long totalCount = localResults.getTotalElements() + offResponse.get().getCount();
        return new PageImpl<>(combined, pageable, totalCount);
    }

    // ── Busca por ID ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Optional<Food> findById(UUID id) {
        return foodRepository.findByIdWithMeasures(id);
    }

    // ── Busca por Barcode (com fallback OFF) ──────────────────────────────────

    @Transactional(readOnly = true)
    public Optional<Food> findByBarcode(String barcode) {
        Optional<Food> local = foodRepository.findByBarcode(barcode);
        if (local.isPresent()) {
            return local;
        }

        log.info("Barcode {} não encontrado localmente. Consultando OFF.", barcode);

        return offClient.findByBarcode(barcode)
                .filter(r -> r.getProduct() != null)
                .map(r -> r.getProduct())
                .filter(this::hasMinimumNutritionalData)
                .map(product -> {
                    Food food = foodMapper.fromOffProduct(product);
                    scheduleAsyncPersistence(List.of(food), "barcode:" + barcode);
                    return food;
                });
    }

    // ── Async Persistence ─────────────────────────────────────────────────────

    /**
     * Agenda a persistência assíncrona dos alimentos da Open Food Facts.
     *
     * <p>Esta operação <b>não bloqueia</b> a thread de resposta ao usuário.
     * Executa no pool {@code @Async("offPersistenceExecutor")} (vide {@link AsyncConfig}).
     *
     * @param foods alimentos a salvar (sem ID — serão gerados pelo banco)
     * @param origin contexto de onde vieram (para logs)
     */
    private void scheduleAsyncPersistence(List<Food> foods, String origin) {
        log.debug("Agendando persistência assíncrona de {} alimentos OFF (origin='{}')",
                foods.size(), origin);
        persistenceService.persistOffFoodsAsync(foods, origin);
    }

    // ── Validation ────────────────────────────────────────────────────────────

    /**
     * Verifica se o produto OFF tem dados mínimos para ser utilizável no FuelMatch.
     * Descarta produtos sem nome ou sem nenhum macro informado.
     */
    private boolean hasMinimumNutritionalData(OffProduct product) {
        if (product.getBestName().isBlank()) return false;
        return product.getEnergyKcal100g() != null
            || product.getProteins100g() != null
            || product.getCarbohydrates100g() != null
            || product.getFat100g() != null;
    }
}