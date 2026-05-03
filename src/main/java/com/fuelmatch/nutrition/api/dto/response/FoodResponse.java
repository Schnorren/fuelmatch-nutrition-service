package com.fuelmatch.nutrition.api.dto.response;

import com.fuelmatch.nutrition.domain.model.Food;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO para alimento — exposto na API REST.
 * Inclui macros por 100g e lista de medidas caseiras disponíveis.
 */
@Data
@NoArgsConstructor
public class FoodResponse {

    private UUID id;
    private String name;
    private String brand;
    private String barcode;
    private Food.FoodSource source;
    private Food.FoodCategory category;

    // Macros por 100g
    private BigDecimal energyKcal;
    private BigDecimal carbohydratesG;
    private BigDecimal ofWhichSugarsG;
    private BigDecimal ofWhichFiberG;
    private BigDecimal proteinsG;
    private BigDecimal fatTotalG;
    private BigDecimal ofWhichSaturatedG;
    private BigDecimal ofWhichTransG;
    private BigDecimal sodiumMg;
    private Map<String, BigDecimal> micronutrients;

    private String ingredientsText;
    private String[] allergens;
    private BigDecimal servingSizeG;
    private String servingDescription;
    private String imageUrl;

    private boolean verified;
    private List<HouseholdMeasureResponse> measures;

    @Data
    @NoArgsConstructor
    public static class HouseholdMeasureResponse {
        private UUID id;
        private String name;
        private BigDecimal quantity;
        private BigDecimal weightG;
        private boolean defaultMeasure;
    }
}
