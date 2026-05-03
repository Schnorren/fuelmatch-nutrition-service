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
    date = "2026-05-02T22:47:54-0300",
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
        food.id( entity.getId() );
        food.name( entity.getName() );
        food.brand( entity.getBrand() );
        food.barcode( entity.getBarcode() );
        food.externalId( entity.getExternalId() );
        food.energyKcal( entity.getEnergyKcal() );
        food.energyKj( entity.getEnergyKj() );
        food.carbohydratesG( entity.getCarbohydratesG() );
        food.ofWhichSugarsG( entity.getOfWhichSugarsG() );
        food.ofWhichFiberG( entity.getOfWhichFiberG() );
        food.proteinsG( entity.getProteinsG() );
        food.fatTotalG( entity.getFatTotalG() );
        food.ofWhichSaturatedG( entity.getOfWhichSaturatedG() );
        food.ofWhichTransG( entity.getOfWhichTransG() );
        food.sodiumMg( entity.getSodiumMg() );
        Map<String, BigDecimal> map = entity.getMicronutrients();
        if ( map != null ) {
            food.micronutrients( new LinkedHashMap<String, BigDecimal>( map ) );
        }
        food.ingredientsText( entity.getIngredientsText() );
        String[] allergens = entity.getAllergens();
        if ( allergens != null ) {
            food.allergens( Arrays.copyOf( allergens, allergens.length ) );
        }
        food.servingSizeG( entity.getServingSizeG() );
        food.servingDescription( entity.getServingDescription() );
        food.imageUrl( entity.getImageUrl() );
        food.tenantId( entity.getTenantId() );
        food.measures( toDomainMeasures( entity.getMeasures() ) );

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

        foodEntity.id( food.getId() );
        foodEntity.name( food.getName() );
        foodEntity.brand( food.getBrand() );
        foodEntity.barcode( food.getBarcode() );
        foodEntity.externalId( food.getExternalId() );
        foodEntity.energyKcal( food.getEnergyKcal() );
        foodEntity.energyKj( food.getEnergyKj() );
        foodEntity.carbohydratesG( food.getCarbohydratesG() );
        foodEntity.ofWhichSugarsG( food.getOfWhichSugarsG() );
        foodEntity.ofWhichFiberG( food.getOfWhichFiberG() );
        foodEntity.proteinsG( food.getProteinsG() );
        foodEntity.fatTotalG( food.getFatTotalG() );
        foodEntity.ofWhichSaturatedG( food.getOfWhichSaturatedG() );
        foodEntity.ofWhichTransG( food.getOfWhichTransG() );
        foodEntity.sodiumMg( food.getSodiumMg() );
        Map<String, BigDecimal> map = food.getMicronutrients();
        if ( map != null ) {
            foodEntity.micronutrients( new LinkedHashMap<String, BigDecimal>( map ) );
        }
        foodEntity.ingredientsText( food.getIngredientsText() );
        String[] allergens = food.getAllergens();
        if ( allergens != null ) {
            foodEntity.allergens( Arrays.copyOf( allergens, allergens.length ) );
        }
        foodEntity.servingSizeG( food.getServingSizeG() );
        foodEntity.servingDescription( food.getServingDescription() );
        foodEntity.imageUrl( food.getImageUrl() );
        foodEntity.active( food.isActive() );
        foodEntity.verified( food.isVerified() );
        foodEntity.tenantId( food.getTenantId() );

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

        foodResponse.setId( food.getId() );
        foodResponse.setName( food.getName() );
        foodResponse.setBrand( food.getBrand() );
        foodResponse.setBarcode( food.getBarcode() );
        foodResponse.setSource( food.getSource() );
        foodResponse.setCategory( food.getCategory() );
        foodResponse.setEnergyKcal( food.getEnergyKcal() );
        foodResponse.setCarbohydratesG( food.getCarbohydratesG() );
        foodResponse.setOfWhichSugarsG( food.getOfWhichSugarsG() );
        foodResponse.setOfWhichFiberG( food.getOfWhichFiberG() );
        foodResponse.setProteinsG( food.getProteinsG() );
        foodResponse.setFatTotalG( food.getFatTotalG() );
        foodResponse.setOfWhichSaturatedG( food.getOfWhichSaturatedG() );
        foodResponse.setOfWhichTransG( food.getOfWhichTransG() );
        foodResponse.setSodiumMg( food.getSodiumMg() );
        Map<String, BigDecimal> map = food.getMicronutrients();
        if ( map != null ) {
            foodResponse.setMicronutrients( new LinkedHashMap<String, BigDecimal>( map ) );
        }
        foodResponse.setIngredientsText( food.getIngredientsText() );
        String[] allergens = food.getAllergens();
        if ( allergens != null ) {
            foodResponse.setAllergens( Arrays.copyOf( allergens, allergens.length ) );
        }
        foodResponse.setServingSizeG( food.getServingSizeG() );
        foodResponse.setServingDescription( food.getServingDescription() );
        foodResponse.setImageUrl( food.getImageUrl() );
        foodResponse.setVerified( food.isVerified() );
        foodResponse.setMeasures( householdMeasureListToHouseholdMeasureResponseList( food.getMeasures() ) );

        return foodResponse;
    }

    @Override
    public MacroResultResponse toResponse(MacroResult macroResult) {
        if ( macroResult == null ) {
            return null;
        }

        MacroResultResponse macroResultResponse = new MacroResultResponse();

        macroResultResponse.setFoodId( macroResult.getFoodId() );
        macroResultResponse.setFoodName( macroResult.getFoodName() );
        macroResultResponse.setBrand( macroResult.getBrand() );
        macroResultResponse.setCalculatedWeightG( macroResult.getCalculatedWeightG() );
        macroResultResponse.setPortionDescription( macroResult.getPortionDescription() );
        macroResultResponse.setEnergyKcal( macroResult.getEnergyKcal() );
        macroResultResponse.setEnergyKj( macroResult.getEnergyKj() );
        macroResultResponse.setCarbohydratesG( macroResult.getCarbohydratesG() );
        macroResultResponse.setOfWhichSugarsG( macroResult.getOfWhichSugarsG() );
        macroResultResponse.setOfWhichFiberG( macroResult.getOfWhichFiberG() );
        macroResultResponse.setProteinsG( macroResult.getProteinsG() );
        macroResultResponse.setFatTotalG( macroResult.getFatTotalG() );
        macroResultResponse.setOfWhichSaturatedG( macroResult.getOfWhichSaturatedG() );
        macroResultResponse.setOfWhichTransG( macroResult.getOfWhichTransG() );
        macroResultResponse.setSodiumMg( macroResult.getSodiumMg() );
        Map<String, BigDecimal> map = macroResult.getMicronutrients();
        if ( map != null ) {
            macroResultResponse.setMicronutrients( new LinkedHashMap<String, BigDecimal>( map ) );
        }

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

        food.name( request.getName() );
        food.brand( request.getBrand() );
        food.barcode( request.getBarcode() );
        food.category( request.getCategory() );
        food.energyKcal( request.getEnergyKcal() );
        food.carbohydratesG( request.getCarbohydratesG() );
        food.ofWhichSugarsG( request.getOfWhichSugarsG() );
        food.ofWhichFiberG( request.getOfWhichFiberG() );
        food.proteinsG( request.getProteinsG() );
        food.fatTotalG( request.getFatTotalG() );
        food.ofWhichSaturatedG( request.getOfWhichSaturatedG() );
        food.sodiumMg( request.getSodiumMg() );
        food.ingredientsText( request.getIngredientsText() );
        food.servingSizeG( request.getServingSizeG() );
        food.servingDescription( request.getServingDescription() );
        food.imageUrl( request.getImageUrl() );
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

        householdMeasureResponse.setId( householdMeasure.getId() );
        householdMeasureResponse.setName( householdMeasure.getName() );
        householdMeasureResponse.setQuantity( householdMeasure.getQuantity() );
        householdMeasureResponse.setWeightG( householdMeasure.getWeightG() );
        householdMeasureResponse.setDefaultMeasure( householdMeasure.isDefaultMeasure() );

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
