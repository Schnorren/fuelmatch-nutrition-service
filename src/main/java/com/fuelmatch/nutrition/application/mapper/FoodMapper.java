package com.fuelmatch.nutrition.application.mapper;

import com.fuelmatch.nutrition.api.dto.response.FoodResponse;
import com.fuelmatch.nutrition.api.dto.response.MacroResultResponse;
import com.fuelmatch.nutrition.api.dto.request.CreateFoodRequest;
import com.fuelmatch.nutrition.domain.model.Food;
import com.fuelmatch.nutrition.domain.model.HouseholdMeasure;
import com.fuelmatch.nutrition.domain.model.MacroResult;
import com.fuelmatch.nutrition.infrastructure.integration.openfoodfacts.dto.OffProductResponse.OffProduct;
import com.fuelmatch.nutrition.infrastructure.persistence.entity.FoodEntity;
import com.fuelmatch.nutrition.infrastructure.persistence.entity.HouseholdMeasureEntity;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper central usando MapStruct (processado em tempo de compilação, zero reflection).
 *
 * <p>Convenções:
 * <ul>
 *   <li>{@code toDomain}: Entity/DTO → Domain Model</li>
 *   <li>{@code toEntity}: Domain Model → JPA Entity</li>
 *   <li>{@code toResponse}: Domain Model → API DTO</li>
 *   <li>{@code fromOffProduct}: OFF DTO → Domain Model</li>
 * </ul>
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface FoodMapper {

    // ── Entity → Domain ───────────────────────────────────────────────────────

    @Mapping(target = "source", expression = "java(Food.FoodSource.valueOf(entity.getSource().name()))")
    @Mapping(target = "category", expression = "java(entity.getCategory() != null ? Food.FoodCategory.valueOf(entity.getCategory().name()) : null)")
    @Mapping(target = "active", source = "active")
    @Mapping(target = "verified", source = "verified")
    Food toDomain(FoodEntity entity);

    @Mapping(target = "defaultMeasure", source = "defaultMeasure")
    HouseholdMeasure toDomain(HouseholdMeasureEntity entity);

    List<HouseholdMeasure> toDomainMeasures(List<HouseholdMeasureEntity> entities);

    // ── Domain → Entity ───────────────────────────────────────────────────────

    @Mapping(target = "source", expression = "java(FoodEntity.FoodSource.valueOf(food.getSource().name()))")
    @Mapping(target = "category", expression = "java(food.getCategory() != null ? FoodEntity.FoodCategory.valueOf(food.getCategory().name()) : null)")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "measures", ignore = true)
    @Mapping(target = "synonyms", ignore = true)
    FoodEntity toEntity(Food food);

    // ── Domain → Response DTO ─────────────────────────────────────────────────

    FoodResponse toResponse(Food food);

    MacroResultResponse toResponse(MacroResult macroResult);

    // ── OFF Product → Domain ──────────────────────────────────────────────────

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", expression = "java(product.getBestName())")
    @Mapping(target = "brand", source = "brands")
    @Mapping(target = "barcode", source = "code")
    @Mapping(target = "source", constant = "OPEN_FOOD_FACTS")
    @Mapping(target = "externalId", source = "code")
    @Mapping(target = "category", expression = "java(resolveCategory(product))")
    @Mapping(target = "energyKcal", expression = "java(product.getEnergyKcal100g())")
    @Mapping(target = "proteinsG", expression = "java(product.getProteins100g())")
    @Mapping(target = "carbohydratesG", expression = "java(product.getCarbohydrates100g())")
    @Mapping(target = "fatTotalG", expression = "java(product.getFat100g())")
    @Mapping(target = "ofWhichFiberG", expression = "java(product.getFiber100g())")
    @Mapping(target = "ofWhichSugarsG", expression = "java(product.getSugars100g())")
    @Mapping(target = "ofWhichSaturatedG", expression = "java(product.getSaturatedFat100g())")
    @Mapping(target = "sodiumMg", expression = "java(product.getSodium100g())")
    @Mapping(target = "servingSizeG", source = "servingQuantity")
    @Mapping(target = "servingDescription", source = "servingSize")
    @Mapping(target = "imageUrl", source = "imageFrontUrl")
    @Mapping(target = "ingredientsText", expression = "java(resolveIngredients(product))")
    @Mapping(target = "allergens", expression = "java(resolveAllergens(product))")
    @Mapping(target = "micronutrients", expression = "java(java.util.Map.of())")
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "verified", constant = "false")
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "measures", expression = "java(java.util.List.of())")
    Food fromOffProduct(OffProduct product);

    // ── CreateFoodRequest → Domain ─────────────────────────────────────────────

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "source", constant = "CUSTOM")
    @Mapping(target = "externalId", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "verified", constant = "false")
    @Mapping(target = "micronutrients", expression = "java(request.getMicronutrients() != null ? request.getMicronutrients() : java.util.Map.of())")
    @Mapping(target = "measures", expression = "java(java.util.List.of())")
    @Mapping(target = "allergens", expression = "java(request.getAllergens() != null ? request.getAllergens() : new String[0])")
    @Mapping(target = "energyKj", ignore = true)
    @Mapping(target = "ofWhichTransG", ignore = true)
    Food fromCreateRequest(CreateFoodRequest request);

    // ── Default / Helper methods ───────────────────────────────────────────────

    default Food.FoodCategory resolveCategory(OffProduct product) {
        if (product.getCategoriesTags() == null) return Food.FoodCategory.INDUSTRIALIZADOS;
        List<String> tags = product.getCategoriesTags();
        if (tags.stream().anyMatch(t -> t.contains("beverages") || t.contains("bebidas")))
            return Food.FoodCategory.BEBIDAS;
        if (tags.stream().anyMatch(t -> t.contains("dairy") || t.contains("laticinios")))
            return Food.FoodCategory.OVOS_LATICINIOS;
        if (tags.stream().anyMatch(t -> t.contains("cereals") || t.contains("cereais")))
            return Food.FoodCategory.CEREAIS_GRAOS;
        if (tags.stream().anyMatch(t -> t.contains("meats") || t.contains("carnes")))
            return Food.FoodCategory.CARNES_AVES;
        return Food.FoodCategory.INDUSTRIALIZADOS;
    }

    default String resolveIngredients(OffProduct product) {
        if (product.getIngredientsTextPt() != null && !product.getIngredientsTextPt().isBlank()) {
            return product.getIngredientsTextPt();
        }
        return product.getIngredientsText();
    }

    default String[] resolveAllergens(OffProduct product) {
        if (product.getAllergensTags() == null) return new String[0];
        return product.getAllergensTags().stream()
                .map(tag -> tag.replace("en:", "").replace("pt:", ""))
                .toArray(String[]::new);
    }
}