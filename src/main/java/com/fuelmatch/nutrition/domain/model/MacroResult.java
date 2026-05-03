package com.fuelmatch.nutrition.domain.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Resultado do cálculo de macronutrientes para uma porção específica.
 *
 * <p>Gerado pelo {@link com.fuelmatch.nutrition.application.service.MacroCalculatorService}.
 * Todos os valores já estão proporcionais à {@code requestedWeightG}.
 */
@Value
@Builder
public class MacroResult {

    UUID foodId;
    String foodName;
    String brand;

    /** Peso em gramas que foi de fato calculado (após resolução de medida). */
    BigDecimal calculatedWeightG;

    /** Descrição da entrada do usuário. Ex: "250g", "2 colheres de sopa". */
    String portionDescription;

    // ── Macros calculados para a porção ──────────────────────────────────────
    BigDecimal energyKcal;
    BigDecimal energyKj;
    BigDecimal carbohydratesG;
    BigDecimal ofWhichSugarsG;
    BigDecimal ofWhichFiberG;
    BigDecimal proteinsG;
    BigDecimal fatTotalG;
    BigDecimal ofWhichSaturatedG;
    BigDecimal ofWhichTransG;
    BigDecimal sodiumMg;

    /** Micronutrientes calculados (mesmo mapa, valores proporcionais). */
    Map<String, BigDecimal> micronutrients;
}
