package com.fuelmatch.nutrition.application.service;

import com.fuelmatch.nutrition.api.dto.request.BatchCalculateRequest;
import com.fuelmatch.nutrition.api.dto.response.BatchCalculateResponse;
import com.fuelmatch.nutrition.api.dto.response.MacroResultResponse;
import com.fuelmatch.nutrition.application.mapper.FoodMapper;
import com.fuelmatch.nutrition.domain.model.Food;
import com.fuelmatch.nutrition.domain.model.MacroResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MacroAggregatorService")
class MacroAggregatorServiceTest {

    @Mock FoodSearchService foodSearchService;
    @Mock MacroCalculatorService calculatorService;
    @Mock FoodMapper foodMapper;

    @InjectMocks MacroAggregatorService aggregatorService;

    private UUID foodId;
    private Food mockFood;
    private MacroResult mockResult;
    private MacroResultResponse mockResponse;

    @BeforeEach
    void setUp() {
        foodId = UUID.randomUUID();

        mockFood = Food.builder()
                .id(foodId)
                .name("Frango grelhado")
                .source(Food.FoodSource.TACO)
                .proteinsG(new BigDecimal("32"))
                .carbohydratesG(BigDecimal.ZERO)
                .fatTotalG(new BigDecimal("3.2"))
                .energyKcal(new BigDecimal("159"))
                .active(true)
                .measures(List.of())
                .build();

        mockResult = MacroResult.builder()
                .foodId(foodId)
                .foodName("Frango grelhado")
                .calculatedWeightG(new BigDecimal("100"))
                .portionDescription("100g")
                .energyKcal(new BigDecimal("159"))
                .proteinsG(new BigDecimal("32"))
                .carbohydratesG(BigDecimal.ZERO)
                .fatTotalG(new BigDecimal("3.2"))
                .sodiumMg(new BigDecimal("74"))
                .build();

        mockResponse = new MacroResultResponse();
        mockResponse.setFoodId(foodId);
        mockResponse.setEnergyKcal(new BigDecimal("159"));
        mockResponse.setProteinsG(new BigDecimal("32"));
        mockResponse.setCarbohydratesG(BigDecimal.ZERO);
        mockResponse.setFatTotalG(new BigDecimal("3.2"));
        mockResponse.setSodiumMg(new BigDecimal("74"));
    }

    @Test
    @DisplayName("Batch com 2 itens válidos retorna resultados e totais corretos")
    void batchTwoValidItems() {
        when(foodSearchService.findById(foodId)).thenReturn(Optional.of(mockFood));
        when(calculatorService.calculate(any(), anyString())).thenReturn(mockResult);
        when(foodMapper.toResponse(any(MacroResult.class))).thenReturn(mockResponse);

        BatchCalculateRequest request = buildRequest(
                List.of(
                        buildItem(foodId, "100g", "item-1"),
                        buildItem(foodId, "100g", "item-2")
                )
        );

        BatchCalculateResponse response = aggregatorService.calculateBatch(request);

        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems()).allMatch(BatchCalculateResponse.BatchItemResult::isSuccess);
        assertThat(response.getErrorCount()).isZero();

        // Totais: 2 × 159 kcal = 318
        assertThat(response.getTotals().getTotalEnergyKcal())
                .isEqualByComparingTo("318.00");
        assertThat(response.getTotals().getTotalProteinsG())
                .isEqualByComparingTo("64.00");
    }

    @Test
    @DisplayName("Alimento não encontrado gera item com success=false e não afeta os demais")
    void notFoundFoodGeneratesErrorItem() {
        UUID unknownId = UUID.randomUUID();
        when(foodSearchService.findById(unknownId)).thenReturn(Optional.empty());
        when(foodSearchService.findById(foodId)).thenReturn(Optional.of(mockFood));
        when(calculatorService.calculate(any(), anyString())).thenReturn(mockResult);
        when(foodMapper.toResponse(any(MacroResult.class))).thenReturn(mockResponse);

        BatchCalculateRequest request = buildRequest(
                List.of(
                        buildItem(foodId, "100g", "valido"),
                        buildItem(unknownId, "100g", "invalido")
                )
        );

        BatchCalculateResponse response = aggregatorService.calculateBatch(request);

        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems().get(0).isSuccess()).isTrue();
        assertThat(response.getItems().get(1).isSuccess()).isFalse();
        assertThat(response.getItems().get(1).getCorrelationId()).isEqualTo("invalido");
        assertThat(response.getErrorCount()).isEqualTo(1);

        // Totais consideram apenas o item válido
        assertThat(response.getTotals().getTotalEnergyKcal())
                .isEqualByComparingTo("159.00");
    }

    @Test
    @DisplayName("PortionResolutionException capturada por item sem cancelar batch")
    void portionResolutionExceptionHandledGracefully() {
        when(foodSearchService.findById(foodId)).thenReturn(Optional.of(mockFood));
        when(calculatorService.calculate(any(), anyString()))
                .thenThrow(new MacroCalculatorService.PortionResolutionException("porção inválida"));

        BatchCalculateRequest request = buildRequest(
                List.of(buildItem(foodId, "xyz inválido", "item-1"))
        );

        BatchCalculateResponse response = aggregatorService.calculateBatch(request);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).isSuccess()).isFalse();
        assertThat(response.getItems().get(0).getErrorMessage()).contains("porção inválida");
        assertThat(response.getErrorCount()).isEqualTo(1);
        // Totais zerados
        assertThat(response.getTotals().getTotalEnergyKcal()).isEqualByComparingTo("0.00");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private BatchCalculateRequest buildRequest(List<BatchCalculateRequest.BatchItem> items) {
        BatchCalculateRequest request = new BatchCalculateRequest();
        request.setItems(items);
        return request;
    }

    private BatchCalculateRequest.BatchItem buildItem(UUID id, String portion, String corrId) {
        BatchCalculateRequest.BatchItem item = new BatchCalculateRequest.BatchItem();
        item.setFoodId(id);
        item.setPortionInput(portion);
        item.setCorrelationId(corrId);
        return item;
    }
}
