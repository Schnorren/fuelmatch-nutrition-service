package com.fuelmatch.nutrition.api.controller;

import com.fuelmatch.nutrition.api.dto.request.BatchCalculateRequest;
import com.fuelmatch.nutrition.api.dto.request.CalculateMacrosRequest;
import com.fuelmatch.nutrition.api.dto.request.CreateFoodRequest;
import com.fuelmatch.nutrition.api.dto.response.BatchCalculateResponse;
import com.fuelmatch.nutrition.api.dto.response.FoodResponse;
import com.fuelmatch.nutrition.api.dto.response.MacroResultResponse;
import com.fuelmatch.nutrition.application.mapper.FoodMapper;
import com.fuelmatch.nutrition.application.service.FoodSearchService;
import com.fuelmatch.nutrition.application.service.MacroAggregatorService;
import com.fuelmatch.nutrition.application.service.MacroCalculatorService;
import com.fuelmatch.nutrition.application.port.FoodRepository;
import com.fuelmatch.nutrition.domain.model.Food;
import com.fuelmatch.nutrition.domain.model.MacroResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller REST para o domínio de alimentos.
 *
 * <p>Base path: {@code /api/v1/foods}
 */
@RestController
@RequestMapping("/api/v1/foods")
@RequiredArgsConstructor
@Validated
@Slf4j
public class FoodController {

    private final FoodSearchService searchService;
    private final MacroCalculatorService calculatorService;
    private final MacroAggregatorService aggregatorService;
    private final FoodRepository foodRepository;
    private final FoodMapper mapper;

    // ── GET /api/v1/foods/search ───────────────────────────────────────────────
    /**
     * Busca alimentos com fuzzy search + fallback Open Food Facts.
     *
     * <p>Query params:
     * <ul>
     *   <li>{@code q} — termo de busca (obrigatório)</li>
     *   <li>{@code source} — filtro: TACO | OPEN_FOOD_FACTS | CUSTOM (opcional)</li>
     *   <li>{@code tenantId} — para incluir alimentos customizados do tenant (opcional)</li>
     *   <li>{@code page} — página (default 0)</li>
     *   <li>{@code size} — itens por página (default 20, max 50)</li>
     * </ul>
     */
    @GetMapping("/search")
    public ResponseEntity<Page<FoodResponse>> search(
            @RequestParam String q,
            @RequestParam(required = false) Food.FoodSource source,
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int safeSize = Math.min(size, 50);
        PageRequest pageable = PageRequest.of(page, safeSize, Sort.unsorted());

        Page<Food> results = searchService.search(q, source, tenantId, pageable);
        Page<FoodResponse> response = results.map(mapper::toResponse);

        log.debug("Search '{}' retornou {} resultados (total: {})",
                q, response.getNumberOfElements(), response.getTotalElements());

        return ResponseEntity.ok(response);
    }

    // ── GET /api/v1/foods/{id} ────────────────────────────────────────────────
    /**
     * Retorna um alimento pelo ID com medidas caseiras.
     */
    @GetMapping("/{id}")
    public ResponseEntity<FoodResponse> findById(@PathVariable UUID id) {
        return searchService.findById(id)
                .map(food -> ResponseEntity.ok(mapper.toResponse(food)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ── GET /api/v1/foods/barcode/{barcode} ───────────────────────────────────
    /**
     * Busca alimento por código de barras com fallback para Open Food Facts.
     */
    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<FoodResponse> findByBarcode(
            @PathVariable @Pattern(regexp = "^[0-9]{8,13}$", message = "Barcode deve ter 8 a 13 dígitos numéricos")
            String barcode) {
        return searchService.findByBarcode(barcode)
                .map(food -> ResponseEntity.ok(mapper.toResponse(food)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ── POST /api/v1/foods ────────────────────────────────────────────────────
    /**
     * Cadastra um alimento customizado (por nutricionista).
     */
    @PostMapping
    public ResponseEntity<FoodResponse> create(
            @Valid @RequestBody CreateFoodRequest request) {
        Food food = mapper.fromCreateRequest(request);
        Food saved = foodRepository.save(food);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toResponse(saved));
    }

    // ── POST /api/v1/foods/calculate ──────────────────────────────────────────
    /**
     * Calcula macros de um alimento para uma porção especificada.
     *
     * <p>Exemplos de {@code portionInput}:
     * <ul>
     *   <li>{@code "250g"}</li>
     *   <li>{@code "2 colheres de sopa"}</li>
     *   <li>{@code "1 porção"}</li>
     * </ul>
     */
    @PostMapping("/calculate")
    public ResponseEntity<MacroResultResponse> calculate(
            @Valid @RequestBody CalculateMacrosRequest request) {

        Food food = searchService.findById(request.getFoodId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Alimento não encontrado: " + request.getFoodId()));

        MacroResult result = calculatorService.calculate(food, request.getPortionInput());
        return ResponseEntity.ok(mapper.toResponse(result));
    }

    // ── DELETE /api/v1/foods/{id} ─────────────────────────────────────────────
    /**
     * Desativa (soft delete) um alimento customizado.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID id) {
        foodRepository.deactivate(id);
    }

    // ── POST /api/v1/foods/calculate/batch ────────────────────────────────────
    /**
     * Calcula macros de múltiplos alimentos em uma única requisição.
     * Retorna resultados individuais + totais agregados da refeição.
     * Máximo de 50 itens por batch. Itens com erro são sinalizados
     * individualmente sem cancelar o batch inteiro.
     */
    @PostMapping("/calculate/batch")
    public ResponseEntity<BatchCalculateResponse> calculateBatch(
            @Valid @RequestBody BatchCalculateRequest request) {
        BatchCalculateResponse response = aggregatorService.calculateBatch(request);
        return ResponseEntity.ok(response);
    }

    // ── Inner exception ───────────────────────────────────────────────────────
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) { super(message); }
    }
}