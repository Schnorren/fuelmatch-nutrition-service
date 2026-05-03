package com.fuelmatch.nutrition.api.dto.response;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resposta para cálculo de macros em batch.
 *
 * <p>Contém os resultados individuais de cada item E os totais agregados
 * da refeição/dia — eliminando cálculos redundantes no frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchCalculateResponse {

    /** Resultados individuais, em mesma ordem que o request. */
    private List<BatchItemResult> items;

    /** Totais agregados de todos os itens da refeição. */
    private MacroTotals totals;

    /** Número de itens com erro de resolução de porção. */
    private int errorCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchItemResult {
        private String correlationId;
        private boolean success;
        private String errorMessage;
        private MacroResultResponse macros;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MacroTotals {
        private BigDecimal totalEnergyKcal;
        private BigDecimal totalProteinsG;
        private BigDecimal totalCarbohydratesG;
        private BigDecimal totalFatTotalG;
        private BigDecimal totalFiberG;
        private BigDecimal totalSodiumMg;
    }
}
