package com.fuelmatch.nutrition.application.service;

import com.fuelmatch.nutrition.application.mapper.FoodMapper;
import com.fuelmatch.nutrition.application.port.FoodRepository;
import com.fuelmatch.nutrition.domain.model.Food;
import com.fuelmatch.nutrition.infrastructure.integration.openfoodfacts.client.OpenFoodFactsClient;
import com.fuelmatch.nutrition.infrastructure.integration.openfoodfacts.dto.OffProductResponse;
import com.fuelmatch.nutrition.infrastructure.integration.openfoodfacts.dto.OffSearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do FoodSearchService.
 * Simula o fallback para a Open Food Facts e a persistência assíncrona.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FoodSearchService — Fallback Logic")
class FoodSearchServiceTest {

    @Mock FoodRepository foodRepository;
    @Mock OpenFoodFactsClient offClient;
    @Mock FoodMapper foodMapper;
    @Mock FoodPersistenceService persistenceService;

    @InjectMocks FoodSearchService searchService;

    private Pageable pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 20);
    }

    @Test
    @DisplayName("Quando banco local tem resultados suficientes, não chama a OFF")
    void shouldNotCallOffWhenLocalResultsAreSufficient() {
        List<Food> localFoods = buildFoods(5);
        Page<Food> localPage = new PageImpl<>(localFoods, pageable, 5);

        when(foodRepository.fuzzySearch(anyString(), any(), any(), any()))
                .thenReturn(localPage);

        Page<Food> result = searchService.search("frango", null, null, pageable);

        assertThat(result.getContent()).hasSize(5);
        verifyNoInteractions(offClient);
        verifyNoInteractions(persistenceService);
    }

    @Test
    @DisplayName("Quando banco local tem 0 resultados, consulta a Open Food Facts")
    void shouldCallOffWhenLocalResultsAreEmpty() {
        Page<Food> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        when(foodRepository.fuzzySearch(anyString(), any(), any(), any()))
                .thenReturn(emptyPage);

        OffSearchResponse offResponse = buildOffResponse(3);
        when(offClient.searchByName(anyString(), anyInt(), anyInt()))
                .thenReturn(Optional.of(offResponse));

        Food offFood = buildFood("Whey Protein", Food.FoodSource.OPEN_FOOD_FACTS);
        when(foodMapper.fromOffProduct(any()))
                .thenReturn(offFood);

        Page<Food> result = searchService.search("whey protein", null, null, pageable);

        assertThat(result.getContent()).hasSize(3);
        verify(offClient).searchByName(eq("whey protein"), eq(1), anyInt());
        verify(persistenceService).persistOffFoodsAsync(anyList(), anyString());
    }

    @Test
    @DisplayName("OFF indisponível — retorna resultados locais sem exceção")
    void shouldReturnLocalResultsWhenOffFails() {
        List<Food> localFoods = buildFoods(1);
        Page<Food> localPage = new PageImpl<>(localFoods, pageable, 1);
        when(foodRepository.fuzzySearch(anyString(), any(), any(), any()))
                .thenReturn(localPage);

        when(offClient.searchByName(anyString(), anyInt(), anyInt()))
                .thenReturn(Optional.empty());

        Page<Food> result = searchService.search("quinoa", null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verifyNoInteractions(persistenceService);
    }

    @Test
    @DisplayName("Busca TACO não deve chamar a OFF (TACO não contém industrializados)")
    void shouldNotCallOffForTacoSearch() {
        Page<Food> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        when(foodRepository.fuzzySearch(anyString(), any(), any(), any()))
                .thenReturn(emptyPage);

        Page<Food> result = searchService.search("arroz", Food.FoodSource.TACO, null, pageable);

        assertThat(result.getContent()).isEmpty();
        verifyNoInteractions(offClient);
    }

    @Test
    @DisplayName("Não agenda persistência de alimentos que já existem localmente (dedup por barcode)")
    void shouldDeduplicateByBarcodeBeforePersisting() {
        // Local tem 1 resultado com barcode conhecido
        Food localFood = buildFoodWithBarcode("Produto X", "7891000100103");
        Page<Food> localPage = new PageImpl<>(List.of(localFood), pageable, 1);
        when(foodRepository.fuzzySearch(anyString(), any(), any(), any()))
                .thenReturn(localPage);

        // OFF retorna o mesmo produto (mesmo barcode)
        OffSearchResponse offResponse = buildOffResponseWithBarcode("7891000100103");
        when(offClient.searchByName(anyString(), anyInt(), anyInt()))
                .thenReturn(Optional.of(offResponse));

        Food offFood = buildFoodWithBarcode("Produto X OFF", "7891000100103");
        when(foodMapper.fromOffProduct(any())).thenReturn(offFood);

        searchService.search("produto x", null, null, pageable);

        // Async persistence deve ser chamado sem o produto duplicado
        verify(persistenceService, times(0))
                .persistOffFoodsAsync(argThat(foods ->
                        foods.stream().anyMatch(f -> "7891000100103".equals(f.getBarcode()))),
                        anyString());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<Food> buildFoods(int count) {
        List<Food> foods = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            foods.add(buildFood("Alimento " + i, Food.FoodSource.TACO));
        }
        return foods;
    }

    private Food buildFood(String name, Food.FoodSource source) {
        return Food.builder()
                .id(UUID.randomUUID())
                .name(name)
                .source(source)
                .energyKcal(new BigDecimal("100"))
                .proteinsG(new BigDecimal("10"))
                .carbohydratesG(new BigDecimal("10"))
                .fatTotalG(new BigDecimal("5"))
                .active(true)
                .verified(false)
                .measures(List.of())
                .build();
    }

    private Food buildFoodWithBarcode(String name, String barcode) {
        return buildFood(name, Food.FoodSource.OPEN_FOOD_FACTS).toBuilder()
                .barcode(barcode).build();
    }

    private OffSearchResponse buildOffResponse(int count) {
        OffSearchResponse response = new OffSearchResponse();
        response.setCount(count);
        List<OffProductResponse.OffProduct> products = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            products.add(buildOffProduct("Product " + i, "123456789000" + i, null));
        }
        response.setProducts(products);
        return response;
    }

    private OffSearchResponse buildOffResponseWithBarcode(String barcode) {
        OffSearchResponse response = new OffSearchResponse();
        response.setCount(1);
        response.setProducts(List.of(buildOffProduct("Produto", barcode, barcode)));
        return response;
    }

    private OffProductResponse.OffProduct buildOffProduct(String name, String code, String barcode) {
        OffProductResponse.OffProduct product = new OffProductResponse.OffProduct();
        // Não é possível setar via reflection aqui sem quebrar o teste;
        // em implementação real, use um construtor ou builder de teste
        return product;
    }
}
