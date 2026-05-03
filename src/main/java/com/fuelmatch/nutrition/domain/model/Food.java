package com.fuelmatch.nutrition.domain.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Modelo de domínio {@code Food} — livre de dependências de framework.
 *
 * <p>Representa um alimento com seus macronutrientes normalizados para <b>100g</b>.
 * Este é o objeto que trafega entre as camadas de aplicação e domínio.
 *
 * <p>Para calcular macros de uma quantidade específica, use o
 * {@link com.fuelmatch.nutrition.application.service.MacroCalculatorService}.
 */
@Value
@Builder(toBuilder = true)
public class Food {

    UUID id;
    String name;
    String brand;
    String barcode;
    FoodSource source;
    String externalId;
    FoodCategory category;

    // Macros por 100g
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

    Map<String, BigDecimal> micronutrients;

    String ingredientsText;
    String[] allergens;
    BigDecimal servingSizeG;
    String servingDescription;
    String imageUrl;

    boolean active;
    boolean verified;
    UUID tenantId;

    List<HouseholdMeasure> measures;

    public enum FoodSource {
        TACO, OPEN_FOOD_FACTS, CUSTOM
    }

    public enum FoodCategory {
        CEREAIS_GRAOS, LEGUMINOSAS, HORTALICAS, FRUTAS,
        CARNES_AVES, CARNES_BOVINAS, PESCADOS, OVOS_LATICINIOS,
        ACUCARES_DOCES, OLEOS_GORDURAS, BEBIDAS,
        ALIMENTOS_PREPARADOS, INDUSTRIALIZADOS, SUPLEMENTOS, OUTROS
    }
}
