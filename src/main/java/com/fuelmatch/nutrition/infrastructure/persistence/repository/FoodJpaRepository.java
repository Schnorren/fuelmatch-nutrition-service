package com.fuelmatch.nutrition.infrastructure.persistence.repository;

import com.fuelmatch.nutrition.infrastructure.persistence.entity.FoodEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FoodJpaRepository extends JpaRepository<FoodEntity, UUID> {

    Optional<FoodEntity> findByBarcode(String barcode);

    boolean existsByBarcode(String barcode);

    Optional<FoodEntity> findBySourceAndExternalId(
            FoodEntity.FoodSource source, String externalId);

    boolean existsBySourceAndExternalId(
            FoodEntity.FoodSource source, String externalId);

    @Query(value = """
        SELECT f.*
        FROM foods f
        WHERE f.is_active = TRUE
          AND (:source IS NULL OR f.source::text = :source)
          AND (:tenantId IS NULL OR f.tenant_id IS NULL
               OR f.tenant_id = CAST(:tenantId AS uuid))
          AND (
                word_similarity(immutable_unaccent(lower(:query)), f.name_unaccented) > 0.15
                OR f.name_unaccented % immutable_unaccent(lower(:query))
                OR EXISTS (
                    SELECT 1 FROM food_synonyms fs
                    WHERE fs.food_id = f.id
                      AND (fs.synonym_unaccented % immutable_unaccent(lower(:query))
                           OR word_similarity(immutable_unaccent(lower(:query)),
                                             fs.synonym_unaccented) > 0.15)
                )
          )
        ORDER BY
            GREATEST(
                word_similarity(immutable_unaccent(lower(:query)), f.name_unaccented),
                similarity(f.name_unaccented, immutable_unaccent(lower(:query))),
                COALESCE((
                    SELECT MAX(GREATEST(
                        similarity(fs.synonym_unaccented, immutable_unaccent(lower(:query))),
                        word_similarity(immutable_unaccent(lower(:query)), fs.synonym_unaccented)
                    ))
                    FROM food_synonyms fs WHERE fs.food_id = f.id
                ), 0)
            ) DESC
        """,
        countQuery = """
        SELECT COUNT(f.id)
        FROM foods f
        WHERE f.is_active = TRUE
          AND (:source IS NULL OR f.source::text = :source)
          AND (:tenantId IS NULL OR f.tenant_id IS NULL
               OR f.tenant_id = CAST(:tenantId AS uuid))
          AND (
                word_similarity(immutable_unaccent(lower(:query)), f.name_unaccented) > 0.15
                OR f.name_unaccented % immutable_unaccent(lower(:query))
                OR EXISTS (
                    SELECT 1 FROM food_synonyms fs
                    WHERE fs.food_id = f.id
                      AND (fs.synonym_unaccented % immutable_unaccent(lower(:query))
                           OR word_similarity(immutable_unaccent(lower(:query)),
                                             fs.synonym_unaccented) > 0.15)
                )
          )
        """,
        nativeQuery = true)
    Page<FoodEntity> fuzzySearch(
            @Param("query") String query,
            @Param("source") String source,
            @Param("tenantId") String tenantId,
            Pageable pageable);

    @Query("""
        SELECT f FROM FoodEntity f
        WHERE f.active = TRUE
          AND lower(f.name) = lower(:name)
          AND (:brand IS NULL OR lower(f.brand) = lower(:brand))
        """)
    List<FoodEntity> findByNameAndBrandIgnoreCase(
            @Param("name") String name,
            @Param("brand") String brand);

    Page<FoodEntity> findByCategoryAndActiveTrue(
            FoodEntity.FoodCategory category, Pageable pageable);

    Page<FoodEntity> findByTenantIdAndActiveTrue(UUID tenantId, Pageable pageable);

    @Query("""
        SELECT DISTINCT f FROM FoodEntity f
        LEFT JOIN FETCH f.measures
        WHERE f.id = :id AND f.active = TRUE
        """)
    Optional<FoodEntity> findByIdWithMeasures(@Param("id") UUID id);
}