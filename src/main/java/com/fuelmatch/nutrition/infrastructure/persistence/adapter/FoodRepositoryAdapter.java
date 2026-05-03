package com.fuelmatch.nutrition.infrastructure.persistence.adapter;

import com.fuelmatch.nutrition.application.mapper.FoodMapper;
import com.fuelmatch.nutrition.application.port.FoodRepository;
import com.fuelmatch.nutrition.domain.model.Food;
import com.fuelmatch.nutrition.infrastructure.persistence.entity.FoodEntity;
import com.fuelmatch.nutrition.infrastructure.persistence.repository.FoodJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter JPA que implementa o port {@link FoodRepository}.
 * Traduz entre o mundo do domínio ({@link Food}) e o mundo JPA ({@link FoodEntity}).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FoodRepositoryAdapter implements FoodRepository {

    private final FoodJpaRepository jpaRepository;
    private final FoodMapper mapper;

    @Override
    public Page<Food> fuzzySearch(String query, Food.FoodSource source,
                                   UUID tenantId, Pageable pageable) {
        String sourceStr = source != null ? source.name() : null;
        String tenantStr = tenantId != null ? tenantId.toString() : null;
        return jpaRepository
                .fuzzySearch(query, sourceStr, tenantStr, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Food> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Food> findByIdWithMeasures(UUID id) {
        return jpaRepository.findByIdWithMeasures(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Food> findByBarcode(String barcode) {
        return jpaRepository.findByBarcode(barcode).map(mapper::toDomain);
    }

    @Override
    public Optional<Food> findBySourceAndExternalId(Food.FoodSource source, String externalId) {
        FoodEntity.FoodSource entitySource = FoodEntity.FoodSource.valueOf(source.name());
        return jpaRepository.findBySourceAndExternalId(entitySource, externalId)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByBarcode(String barcode) {
        return jpaRepository.existsByBarcode(barcode);  // delegado ao JpaRepository
    }

    @Override
    public boolean existsBySourceAndExternalId(Food.FoodSource source, String externalId) {
        return jpaRepository.existsBySourceAndExternalId(
                FoodEntity.FoodSource.valueOf(source.name()), externalId);
    }

    @Override
    public Food save(Food food) {
        FoodEntity entity = mapper.toEntity(food);
        FoodEntity saved = jpaRepository.save(entity);
        log.debug("Alimento persistido: id={}, name={}", saved.getId(), saved.getName());
        return mapper.toDomain(saved);
    }

    @Override
    public void deactivate(UUID id) {
        jpaRepository.findById(id).ifPresent(entity -> {
            entity.setActive(false);
            jpaRepository.save(entity);
        });
    }
}
