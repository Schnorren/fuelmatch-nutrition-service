package com.fuelmatch.nutrition.api.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Resultado do cálculo de macros para uma porção específica.
 * Todos os valores são proporcionais ao peso calculado.
 */
@Data
@NoArgsConstructor
public class MacroResultResponse {

    private UUID foodId;
    private String foodName;
    private String brand;

    /** Peso em gramas efetivamente calculado. */
    private BigDecimal calculatedWeightG;

    /** Descrição da porção informada pelo usuário. Ex: "2 colheres de sopa (30g)". */
    private String portionDescription;

    // Macros calculados para a porção
    private BigDecimal energyKcal;
    private BigDecimal energyKj;
    private BigDecimal carbohydratesG;
    private BigDecimal ofWhichSugarsG;
    private BigDecimal ofWhichFiberG;
    private BigDecimal proteinsG;
    private BigDecimal fatTotalG;
    private BigDecimal ofWhichSaturatedG;
    private BigDecimal ofWhichTransG;
    private BigDecimal sodiumMg;

    private Map<String, BigDecimal> micronutrients;
}
