package com.fuelmatch.nutrition.infrastructure.integration.openfoodfacts.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DTO raiz para resposta de produto único da Open Food Facts API v2.
 * {@code GET /api/v2/product/{barcode}}
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OffProductResponse {

    private int status;

    @JsonProperty("status_verbose")
    private String statusVerbose;

    private OffProduct product;

    // ── Nested: produto ───────────────────────────────────────────────────────
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OffProduct {

        private String code;

        @JsonProperty("product_name")
        private String productName;

        @JsonAlias({"product_name_pt", "product_name_en"})
        @JsonProperty("product_name_pt")
        private String productNamePt;

        private String brands;

        @JsonProperty("serving_size")
        private String servingSize;

        @JsonProperty("serving_quantity")
        private BigDecimal servingQuantity;

        @JsonProperty("ingredients_text_pt")
        private String ingredientsTextPt;

        @JsonProperty("ingredients_text")
        private String ingredientsText;

        @JsonProperty("allergens_tags")
        private List<String> allergensTags;

        @JsonProperty("categories_tags")
        private List<String> categoriesTags;

        @JsonProperty("image_front_url")
        private String imageFrontUrl;

        /** Mapa completo de nutrientes. Keys seguem padrão OFF: "energy-kcal_100g", etc. */
        private Map<String, Object> nutriments;

        // ── Helper methods para extrair nutrientes do mapa ─────────────────────
        public BigDecimal getNutriment(String key) {
            if (nutriments == null) return null;
            Object val = nutriments.get(key);
            if (val == null) return null;
            try {
                return new BigDecimal(val.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }

        public BigDecimal getEnergyKcal100g() {
            BigDecimal v = getNutriment("energy-kcal_100g");
            return v != null ? v : getNutriment("energy-kcal");
        }

        public BigDecimal getProteins100g() {
            BigDecimal v = getNutriment("proteins_100g");
            return v != null ? v : getNutriment("proteins");
        }

        public BigDecimal getCarbohydrates100g() {
            BigDecimal v = getNutriment("carbohydrates_100g");
            return v != null ? v : getNutriment("carbohydrates");
        }

        public BigDecimal getFat100g() {
            BigDecimal v = getNutriment("fat_100g");
            return v != null ? v : getNutriment("fat");
        }

        public BigDecimal getFiber100g() {
            BigDecimal v = getNutriment("fiber_100g");
            return v != null ? v : getNutriment("fiber");
        }

        public BigDecimal getSugars100g() {
            BigDecimal v = getNutriment("sugars_100g");
            return v != null ? v : getNutriment("sugars");
        }

        public BigDecimal getSaturatedFat100g() {
            BigDecimal v = getNutriment("saturated-fat_100g");
            return v != null ? v : getNutriment("saturated-fat");
        }

        public BigDecimal getSodium100g() {
            BigDecimal v = getNutriment("sodium_100g");
            return v != null ? v : getNutriment("sodium");
        }

        /** Retorna o nome do produto preferindo PT > EN > genérico. */
        public String getBestName() {
            if (productNamePt != null && !productNamePt.isBlank()) return productNamePt;
            if (productName != null && !productName.isBlank()) return productName;
            return "Produto " + code;
        }
    }
}
