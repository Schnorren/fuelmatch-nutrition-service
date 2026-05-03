package com.fuelmatch.nutrition.application.mapper;

import com.fuelmatch.nutrition.api.dto.request.CreateFoodRequest;
import com.fuelmatch.nutrition.api.dto.response.FoodResponse;
import com.fuelmatch.nutrition.api.dto.response.MacroResultResponse;
import com.fuelmatch.nutrition.domain.model.Food;
import com.fuelmatch.nutrition.domain.model.HouseholdMeasure;
import com.fuelmatch.nutrition.domain.model.MacroResult;
import com.fuelmatch.nutrition.infrastructure.integration.openfoodfacts.dto.OffProductResponse;
import com.fuelmatch.nutrition.infrastructure.persistence.entity.FoodEntity;
import com.fuelmatch.nutrition.infrastructure.persistence.entity.HouseholdMeasureEntity;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-02T22:59:57-0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class FoodMapperImpl implements FoodMapper {

    @Override
    public Food toDomain(FoodEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Food.FoodBuilder food = Food.builder();

        if ( entity.getActive() != null ) {
            food.active( entity.getActive() );
        }
        if ( entity.getVerified() != null ) {
            food.verified( entity.getVerified() );
        }
        String[] allergens = entity.getAllergens();
        if ( allergens != null ) {
            food.allergens( Arrays.copyOf( allergens, allergens.length ) );
        }
        food.barcode( entity.getBarcode() );
        food.brand( entity.getBrand() );
        food.carbohydratesG( entity.getCarbohydratesG() );
        food.energyKcal( entity.getEnergyKcal() );
        food.energyKj( entity.getEnergyKj() );
        food.externalId( entity.getExternalId() );
        food.fatTotalG( entity.getFatTotalG() );
        food.id( entity.getId() );
        food.imageUrl( entity.getImageUrl() );
        food.ingredientsText( entity.getIngredientsText() );
        food.measures( toDomainMeasures( entity.getMeasures() ) );
        Map<String, BigDecimal> map = entity.getMicronutrients();
        if ( map != null ) {
            food.micronutrients( new LinkedHashMap<String, BigDecimal>( map ) );
        }
        food.name( entity.getName() );
        food.ofWhichFiberG( entity.getOfWhichFiberG() );
        food.ofWhichSaturatedG( entity.getOfWhichSaturatedG() );
        food.ofWhichSugarsG( entity.getOfWhichSugarsG() );
        food.ofWhichTransG( entity.getOfWhichTransG() );
        food.proteinsG( entity.getProteinsG() );
        food.servingDescription( entity.getServingDescription() );
        food.servingSizeG( entity.getServingSizeG() );
        food.sodiumMg( entity.getSodiumMg() );
        food.tenantId( entity.getTenantId() );

        food.source( Food.FoodSource.valueOf(entity.getSource().name()) );
        food.category( entity.getCategory() != null ? Food.FoodCategory.valueOf(entity.getCategory().name()) : null );

        return food.build();
    }

    @Override
    public HouseholdMeasure toDomain(HouseholdMeasureEntity entity) {
        if ( entity == null ) {
            return null;
        }

        HouseholdMeasure.HouseholdMeasureBuilder householdMeasure = HouseholdMeasure.builder();

        if ( entity.getDefaultMeasure() != null ) {
            householdMeasure.defaultMeasure( entity.getDefaultMeasure() );
        }
        householdMeasure.id( entity.getId() );
        householdMeasure.name( entity.getName() );
        householdMeasure.quantity( entity.getQuantity() );
        householdMeasure.weightG( entity.getWeightG() );

        return householdMeasure.build();
    }

    @Override
    public List<HouseholdMeasure> toDomainMeasures(List<HouseholdMeasureEntity> entities) {
        if ( entities == null ) {
            return null;
        }

        List<HouseholdMeasure> list = new ArrayList<HouseholdMeasure>( entities.size() );
        for ( HouseholdMeasureEntity householdMeasureEntity : entities ) {
            list.add( toDomain( householdMeasureEntity ) );
        }

        return list;
    }

    @Override
    public FoodEntity toEntity(Food food) {
        if ( food == null ) {
            return null;
        }

        FoodEntity.FoodEntityBuilder foodEntity = FoodEntity.builder();

        foodEntity.active( food.isActive() );
        String[] allergens = food.getAllergens();
        if ( allergens != null ) {
            foodEntity.allergens( Arrays.copyOf( allergens, allergens.length ) );
        }
        foodEntity.barcode( food.getBarcode() );
        foodEntity.brand( food.getBrand() );
        foodEntity.carbohydratesG( food.getCarbohydratesG() );
        foodEntity.energyKcal( food.getEnergyKcal() );
        foodEntity.energyKj( food.getEnergyKj() );
        foodEntity.externalId( food.getExternalId() );
        foodEntity.fatTotalG( food.getFatTotalG() );
        foodEntity.id( food.getId() );
        foodEntity.imageUrl( food.getImageUrl() );
        foodEntity.ingredientsText( food.getIngredientsText() );
        Map<String, BigDecimal> map = food.getMicronutrients();
        if ( map != null ) {
            foodEntity.micronutrients( new LinkedHashMap<String, BigDecimal>( map ) );
        }
        foodEntity.name( food.getName() );
        foodEntity.ofWhichFiberG( food.getOfWhichFiberG() );
        foodEntity.ofWhichSaturatedG( food.getOfWhichSaturatedG() );
        foodEntity.ofWhichSugarsG( food.getOfWhichSugarsG() );
        foodEntity.ofWhichTransG( food.getOfWhichTransG() );
        foodEntity.proteinsG( food.getProteinsG() );
        foodEntity.servingDescription( food.getServingDescription() );
        foodEntity.servingSizeG( food.getServingSizeG() );
        foodEntity.sodiumMg( food.getSodiumMg() );
        foodEntity.tenantId( food.getTenantId() );
        foodEntity.verified( food.isVerified() );

        foodEntity.source( FoodEntity.FoodSource.valueOf(food.getSource().name()) );
        foodEntity.category( food.getCategory() != null ? FoodEntity.FoodCategory.valueOf(food.getCategory().name()) : null );

        return foodEntity.build();
    }

    @Override
    public FoodResponse toResponse(Food food) {
        if ( food == null ) {
            return null;
        }

        FoodResponse foodResponse = new FoodResponse();

        String[] allergens = food.getAllergens();
        if ( allergens != null ) {
            foodResponse.setAllergens( Arrays.copyOf( allergens, allergens.length ) );
        }
        foodResponse.setBarcode( food.getBarcode() );
        foodResponse.setBrand( food.getBrand() );
        foodResponse.setCarbohydratesG( food.getCarbohydratesG() );
        foodResponse.setCategory( food.getCategory() );
        foodResponse.setEnergyKcal( food.getEnergyKcal() );
        foodResponse.setFatTotalG( food.getFatTotalG() );
        foodResponse.setId( food.getId() );
        foodResponse.setImageUrl( food.getImageUrl() );
        foodResponse.setIngredientsText( food.getIngredientsText() );
        foodResponse.setMeasures( householdMeasureListToHouseholdMeasureResponseList( food.getMeasures() ) );
        Map<String, BigDecimal> map = food.getMicronutrients();
        if ( map != null ) {
            foodResponse.setMicronutrients( new LinkedHashMap<String, BigDecimal>( map ) );
        }
        foodResponse.setName( food.getName() );
        foodResponse.setOfWhichFiberG( food.getOfWhichFiberG() );
        foodResponse.setOfWhichSaturatedG( food.getOfWhichSaturatedG() );
        foodResponse.setOfWhichSugarsG( food.getOfWhichSugarsG() );
        foodResponse.setOfWhichTransG( food.getOfWhichTransG() );
        foodResponse.setProteinsG( food.getProteinsG() );
        foodResponse.setServingDescription( food.getServingDescription() );
        foodResponse.setServingSizeG( food.getServingSizeG() );
        foodResponse.setSodiumMg( food.getSodiumMg() );
        foodResponse.setSource( food.getSource() );
        foodResponse.setVerified( food.isVerified() );

        return foodResponse;
    }

    @Override
    public MacroResultResponse toResponse(MacroResult macroResult) {
        if ( macroResult == null ) {
            return null;
        }

        MacroResultResponse macroResultResponse = new MacroResultResponse();

        macroResultResponse.setBrand( macroResult.getBrand() );
        macroResultResponse.setCalculatedWeightG( macroResult.getCalculatedWeightG() );
        macroResultResponse.setCarbohydratesG( macroResult.getCarbohydratesG() );
        macroResultResponse.setEnergyKcal( macroResult.getEnergyKcal() );
        macroResultResponse.setEnergyKj( macroResult.getEnergyKj() );
        macroResultResponse.setFatTotalG( macroResult.getFatTotalG() );
        macroResultResponse.setFoodId( macroResult.getFoodId() );
        macroResultResponse.setFoodName( macroResult.getFoodName() );
        Map<String, BigDecimal> map = macroResult.getMicronutrients();
        if ( map != null ) {
            macroResultResponse.setMicronutrients( new LinkedHashMap<String, BigDecimal>( map ) );
        }
        macroResultResponse.setOfWhichFiberG( macroResult.getOfWhichFiberG() );
        macroResultResponse.setOfWhichSaturatedG( macroResult.getOfWhichSaturatedG() );
        macroResultResponse.setOfWhichSugarsG( macroResult.getOfWhichSugarsG() );
        macroResultResponse.setOfWhichTransG( macroResult.getOfWhichTransG() );
        macroResultResponse.setPortionDescription( macroResult.getPortionDescription() );
        macroResultResponse.setProteinsG( macroResult.getProteinsG() );
        macroResultResponse.setSodiumMg( macroResult.getSodiumMg() );

        return macroResultResponse;
    }

    @Override
    public Food fromOffProduct(OffProductResponse.OffProduct product) {
        if ( product == null ) {
            return null;
        }

        Food.FoodBuilder food = Food.builder();

        food.brand( product.getBrands() );
        food.barcode( product.getCode() );
        food.externalId( product.getCode() );
        food.servingSizeG( product.getServingQuantity() );
        food.servingDescription( product.getServingSize() );
        food.imageUrl( product.getImageFrontUrl() );

        food.name( product.getBestName() );
        food.source( Food.FoodSource.OPEN_FOOD_FACTS );
        food.category( resolveCategory(product) );
        food.energyKcal( product.getEnergyKcal100g() );
        food.proteinsG( product.getProteins100g() );
        food.carbohydratesG( product.getCarbohydrates100g() );
        food.fatTotalG( product.getFat100g() );
        food.ofWhichFiberG( product.getFiber100g() );
        food.ofWhichSugarsG( product.getSugars100g() );
        food.ofWhichSaturatedG( product.getSaturatedFat100g() );
        food.sodiumMg( product.getSodium100g() );
        food.ingredientsText( resolveIngredients(product) );
        food.allergens( resolveAllergens(product) );
        food.micronutrients( java.util.Map.of() );
        food.active( true );
        food.verified( false );
        food.measures( java.util.List.of() );

        return food.build();
    }

    @Override
    public Food fromCreateRequest(CreateFoodRequest request) {
        if ( request == null ) {
            return null;
        }

        Food.FoodBuilder food = Food.builder();

        food.barcode( request.getBarcode() );
        food.brand( request.getBrand() );
        food.carbohydratesG( request.getCarbohydratesG() );
        food.category( request.getCategory() );
        food.energyKcal( request.getEnergyKcal() );
        food.fatTotalG( request.getFatTotalG() );
        food.imageUrl( request.getImageUrl() );
        food.ingredientsText( request.getIngredientsText() );
        food.name( request.getName() );
        food.ofWhichFiberG( request.getOfWhichFiberG() );
        food.ofWhichSaturatedG( request.getOfWhichSaturatedG() );
        food.ofWhichSugarsG( request.getOfWhichSugarsG() );
        food.proteinsG( request.getProteinsG() );
        food.servingDescription( request.getServingDescription() );
        food.servingSizeG( request.getServingSizeG() );
        food.sodiumMg( request.getSodiumMg() );
        food.tenantId( request.getTenantId() );

        food.source( Food.FoodSource.CUSTOM );
        food.active( true );
        food.verified( false );
        food.micronutrients( request.getMicronutrients() != null ? request.getMicronutrients() : java.util.Map.of() );
        food.measures( java.util.List.of() );
        food.allergens( request.getAllergens() != null ? request.getAllergens() : new String[0] );

        return food.build();
    }

    protected FoodResponse.HouseholdMeasureResponse householdMeasureToHouseholdMeasureResponse(HouseholdMeasure householdMeasure) {
        if ( householdMeasure == null ) {
            return null;
        }

        FoodResponse.HouseholdMeasureResponse householdMeasureResponse = new FoodResponse.HouseholdMeasureResponse();

        householdMeasureResponse.setDefaultMeasure( householdMeasure.isDefaultMeasure() );
        householdMeasureResponse.setId( householdMeasure.getId() );
        householdMeasureResponse.setName( householdMeasure.getName() );
        householdMeasureResponse.setQuantity( householdMeasure.getQuantity() );
        householdMeasureResponse.setWeightG( householdMeasure.getWeightG() );

        return householdMeasureResponse;
    }

    protected List<FoodResponse.HouseholdMeasureResponse> householdMeasureListToHouseholdMeasureResponseList(List<HouseholdMeasure> list) {
        if ( list == null ) {
            return null;
        }

        List<FoodResponse.HouseholdMeasureResponse> list1 = new ArrayList<FoodResponse.HouseholdMeasureResponse>( list.size() );
        for ( HouseholdMeasure householdMeasure : list ) {
            list1.add( householdMeasureToHouseholdMeasureResponse( householdMeasure ) );
        }

        return list1;
    }
}
