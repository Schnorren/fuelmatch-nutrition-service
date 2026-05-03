package com.fuelmatch.nutrition.application.service;

import com.fuelmatch.nutrition.api.dto.request.BatchCalculateRequest;
import com.fuelmatch.nutrition.api.dto.response.BatchCalculateResponse;
import com.fuelmatch.nutrition.api.dto.response.MacroResultResponse;
import com.fuelmatch.nutrition.application.mapper.FoodMapper;
import com.fuelmatch.nutrition.domain.model.Food;
import com.fuelmatch.nutrition.domain.model.MacroResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orquestra o cálculo de macros em batch para montagem de refeições e planos alimentares.
 *
 * <p>Responsabilidades:
 * <ol>
 *   <li>Iterar sobre cada item do batch delegando ao {@link MacroCalculatorService}</li>
 *   <li>Tratar erros por item sem interromper o batch completo</li>
 *   <li>Agregar os totais de macros da refeição</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MacroAggregatorService {

    private final FoodSearchService foodSearchService;
    private final MacroCalculatorService calculatorService;
    private final FoodMapper foodMapper;

    /**
     * Processa um batch de cálculos e retorna resultados individuais + totais agregados.
     *
     * @param request batch com lista de {foodId, portionInput}
     * @return resposta com macros individuais e totais da refeição
     */
    public BatchCalculateResponse calculateBatch(BatchCalculateRequest request) {
        List<BatchCalculateResponse.BatchItemResult> results = new ArrayList<>();
        int errorCount = 0;

        for (BatchCalculateRequest.BatchItem item : request.getItems()) {
            BatchCalculateResponse.BatchItemResult itemResult = processItem(item);
            results.add(itemResult);
            if (!itemResult.isSuccess()) errorCount++;
        }

        BatchCalculateResponse.MacroTotals totals = aggregateTotals(results);

        log.debug("Batch calculado: {} itens, {} erros", results.size(), errorCount);

        return BatchCalculateResponse.builder()
                .items(results)
                .totals(totals)
                .errorCount(errorCount)
                .build();
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private BatchCalculateResponse.BatchItemResult processItem(
            BatchCalculateRequest.BatchItem item) {
        try {
            Optional<Food> food = foodSearchService.findById(item.getFoodId());
            if (food.isEmpty()) {
                return BatchCalculateResponse.BatchItemResult.builder()
                        .correlationId(item.getCorrelationId())
                        .success(false)
                        .errorMessage("Alimento não encontrado: " + item.getFoodId())
                        .build();
            }

            MacroResult result = calculatorService.calculate(food.get(), item.getPortionInput());
            MacroResultResponse response = foodMapper.toResponse(result);

            return BatchCalculateResponse.BatchItemResult.builder()
                    .correlationId(item.getCorrelationId())
                    .success(true)
                    .macros(response)
                    .build();

        } catch (MacroCalculatorService.PortionResolutionException e) {
            log.debug("Erro de resolução de porção no batch item {}: {}",
                    item.getFoodId(), e.getMessage());
            return BatchCalculateResponse.BatchItemResult.builder()
                    .correlationId(item.getCorrelationId())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("Erro inesperado no batch item {}: {}", item.getFoodId(), e.getMessage());
            return BatchCalculateResponse.BatchItemResult.builder()
                    .correlationId(item.getCorrelationId())
                    .success(false)
                    .errorMessage("Erro interno ao processar item")
                    .build();
        }
    }

    /**
     * Soma os macros de todos os itens bem-sucedidos.
     */
    private BatchCalculateResponse.MacroTotals aggregateTotals(
            List<BatchCalculateResponse.BatchItemResult> results) {

        BigDecimal totalKcal = BigDecimal.ZERO;
        BigDecimal totalProtein = BigDecimal.ZERO;
        BigDecimal totalCarbs = BigDecimal.ZERO;
        BigDecimal totalFat = BigDecimal.ZERO;
        BigDecimal totalFiber = BigDecimal.ZERO;
        BigDecimal totalSodium = BigDecimal.ZERO;

        for (var item : results) {
            if (!item.isSuccess() || item.getMacros() == null) continue;
            MacroResultResponse m = item.getMacros();

            totalKcal   = add(totalKcal,   m.getEnergyKcal());
            totalProtein = add(totalProtein, m.getProteinsG());
            totalCarbs  = add(totalCarbs,  m.getCarbohydratesG());
            totalFat    = add(totalFat,    m.getFatTotalG());
            totalFiber  = add(totalFiber,  m.getOfWhichFiberG());
            totalSodium = add(totalSodium, m.getSodiumMg());
        }

        return BatchCalculateResponse.MacroTotals.builder()
                .totalEnergyKcal(round(totalKcal))
                .totalProteinsG(round(totalProtein))
                .totalCarbohydratesG(round(totalCarbs))
                .totalFatTotalG(round(totalFat))
                .totalFiberG(round(totalFiber))
                .totalSodiumMg(round(totalSodium))
                .build();
    }

    private BigDecimal add(BigDecimal a, BigDecimal b) {
        return b == null ? a : a.add(b);
    }

    private BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
