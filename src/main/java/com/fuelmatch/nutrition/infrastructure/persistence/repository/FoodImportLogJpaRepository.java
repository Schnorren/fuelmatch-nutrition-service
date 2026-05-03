package com.fuelmatch.nutrition.infrastructure.persistence.repository;

import com.fuelmatch.nutrition.infrastructure.persistence.entity.FoodImportLogEntity;
import com.fuelmatch.nutrition.infrastructure.persistence.entity.FoodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface FoodImportLogJpaRepository extends JpaRepository<FoodImportLogEntity, Long> {

    List<FoodImportLogEntity> findBySourceAndStatus(
            FoodEntity.FoodSource source, String status);

    @Query("""
        SELECT COUNT(l) FROM FoodImportLogEntity l
        WHERE l.source = :source
          AND l.status = 'SUCCESS'
          AND l.importedAt >= :since
        """)
    long countSuccessfulImports(
            @Param("source") FoodEntity.FoodSource source,
            @Param("since") OffsetDateTime since);
}
