package com.fuelmatch.nutrition.api.dto.request;

import com.fuelmatch.nutrition.domain.model.Food;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request para cadastro de alimento customizado por nutricionista.
 */
@Data
@NoArgsConstructor
public class CreateFoodRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 300)
    private String name;

    @Size(max = 200)
    private String brand;

    @Pattern(regexp = "^[0-9]{8,13}$", message = "Barcode deve ter 8 a 13 dígitos")
    private String barcode;

    private Food.FoodCategory category;

    // Macros por 100g
    @DecimalMin(value = "0.0", message = "Energia não pode ser negativa")
    private BigDecimal energyKcal;

    @DecimalMin("0.0") private BigDecimal carbohydratesG;
    @DecimalMin("0.0") private BigDecimal ofWhichSugarsG;
    @DecimalMin("0.0") private BigDecimal ofWhichFiberG;
    @DecimalMin("0.0") private BigDecimal proteinsG;
    @DecimalMin("0.0") private BigDecimal fatTotalG;
    @DecimalMin("0.0") private BigDecimal ofWhichSaturatedG;
    @DecimalMin("0.0") private BigDecimal ofWhichTransG;
    @DecimalMin("0.0") private BigDecimal sodiumMg;

    private Map<String, BigDecimal> micronutrients;

    private String ingredientsText;
    private String[] allergens;

    @DecimalMin("0.01")
    private BigDecimal servingSizeG;
    private String servingDescription;
    private String imageUrl;

    @Valid
    private List<HouseholdMeasureRequest> measures;

    private UUID tenantId;

    @Data
    @NoArgsConstructor
    public static class HouseholdMeasureRequest {
        @NotBlank private String name;
        @DecimalMin("0.01") private BigDecimal quantity = BigDecimal.ONE;
        @NotNull @DecimalMin("0.01") private BigDecimal weightG;
        private boolean defaultMeasure;
    }
}
