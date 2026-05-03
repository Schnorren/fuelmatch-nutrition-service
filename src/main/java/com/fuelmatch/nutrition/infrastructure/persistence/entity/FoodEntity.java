package com.fuelmatch.nutrition.infrastructure.persistence.entity;

import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Entidade JPA que mapeia a tabela {@code foods}.
 *
 * <p>Todos os valores nutricionais são armazenados por <b>100g / 100ml</b>.
 * O motor de cálculo ({@link com.fuelmatch.nutrition.application.service.MacroCalculatorService})
 * converte para a quantidade desejada via regra de três.
 */
@Entity
@Table(name = "foods")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"measures", "synonyms"})
@EqualsAndHashCode(of = "id")
public class FoodEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 300)
    private String name;

    @Column(length = 200)
    private String brand;

    @Column(length = 50, unique = true)
    private String barcode;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "food_source")
    private FoodSource source;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "food_category")
    private FoodCategory category;

    // ── Macronutrientes por 100g ──────────────────────────────────────────────

    @Column(name = "energy_kcal", precision = 8, scale = 2)
    private BigDecimal energyKcal;

    @Column(name = "energy_kj", precision = 8, scale = 2)
    private BigDecimal energyKj;

    @Column(name = "carbohydrates_g", precision = 8, scale = 2)
    private BigDecimal carbohydratesG;

    @Column(name = "of_which_sugars_g", precision = 8, scale = 2)
    private BigDecimal ofWhichSugarsG;

    @Column(name = "of_which_fiber_g", precision = 8, scale = 2)
    private BigDecimal ofWhichFiberG;

    @Column(name = "proteins_g", precision = 8, scale = 2)
    private BigDecimal proteinsG;

    @Column(name = "fat_total_g", precision = 8, scale = 2)
    private BigDecimal fatTotalG;

    @Column(name = "of_which_saturated_g", precision = 8, scale = 2)
    private BigDecimal ofWhichSaturatedG;

    @Column(name = "of_which_trans_g", precision = 8, scale = 2)
    private BigDecimal ofWhichTransG;

    @Column(name = "sodium_mg", precision = 8, scale = 2)
    private BigDecimal sodiumMg;

    // ── Micronutrientes JSONB ─────────────────────────────────────────────────
    @Type(JsonBinaryType.class)
    @Column(name = "micronutrients", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, BigDecimal> micronutrients = Map.of();

    // ── Metadados Comerciais ──────────────────────────────────────────────────
    @Column(name = "ingredients_text", columnDefinition = "TEXT")
    private String ingredientsText;

    @Type(StringArrayType.class)
    @Column(name = "allergens", columnDefinition = "_text")
    private String[] allergens;

    @Column(name = "serving_size_g", precision = 8, scale = 2)
    private BigDecimal servingSizeG;

    @Column(name = "serving_description", length = 100)
    private String servingDescription;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // ── Controle ──────────────────────────────────────────────────────────────
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean verified = false;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime updatedAt;

    // ── Relacionamentos ───────────────────────────────────────────────────────
    @OneToMany(mappedBy = "food", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<HouseholdMeasureEntity> measures = new ArrayList<>();

    @OneToMany(mappedBy = "food", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FoodSynonymEntity> synonyms = new ArrayList<>();

    // ── Lifecycle Hooks ───────────────────────────────────────────────────────
    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // ── Helper Methods ────────────────────────────────────────────────────────
    public void addMeasure(HouseholdMeasureEntity measure) {
        measure.setFood(this);
        this.measures.add(measure);
    }

    public void addSynonym(FoodSynonymEntity synonym) {
        synonym.setFood(this);
        this.synonyms.add(synonym);
    }

    // ── Enums (inner) ─────────────────────────────────────────────────────────
    public enum FoodSource {
        TACO, OPEN_FOOD_FACTS, CUSTOM
    }

    public enum FoodCategory {
        CEREAIS_GRAOS, LEGUMINOSAS, HORTALICAS, FRUTAS,
        CARNES_AVES, CARNES_BOVINAS, PESCADOS, OVOS_LATICINIOS,
        ACUCARES_DOCES, OLEOS_GORDURAS, BEBIDAS,
        ALIMENTOS_PREPARADOS, INDUSTRIALIZADOS, SUPLEMENTOS, OUTROS
    }
}