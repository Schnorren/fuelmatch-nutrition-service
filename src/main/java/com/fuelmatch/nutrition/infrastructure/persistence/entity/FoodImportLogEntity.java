package com.fuelmatch.nutrition.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Registro de auditoria para importações de alimentos (TACO batch, OFF cache).
 * Permite rastrear falhas, duplicatas e volume de cache warming.
 */
@Entity
@Table(name = "food_import_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodImportLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "food_id")
    private UUID foodId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, columnDefinition = "food_source")
    private FoodEntity.FoodSource source;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // SUCCESS | FAILED | DUPLICATE

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "imported_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime importedAt;

    @PrePersist
    void onCreate() {
        this.importedAt = OffsetDateTime.now();
    }

    // ── Factory methods ───────────────────────────────────────────────────────

    public static FoodImportLogEntity success(UUID foodId,
                                               FoodEntity.FoodSource source,
                                               String externalId) {
        return FoodImportLogEntity.builder()
                .foodId(foodId)
                .source(source)
                .externalId(externalId)
                .status("SUCCESS")
                .build();
    }

    public static FoodImportLogEntity duplicate(FoodEntity.FoodSource source,
                                                 String externalId) {
        return FoodImportLogEntity.builder()
                .source(source)
                .externalId(externalId)
                .status("DUPLICATE")
                .build();
    }

    public static FoodImportLogEntity failed(FoodEntity.FoodSource source,
                                              String externalId,
                                              String errorMessage) {
        return FoodImportLogEntity.builder()
                .source(source)
                .externalId(externalId)
                .status("FAILED")
                .errorMessage(errorMessage)
                .build();
    }
}
