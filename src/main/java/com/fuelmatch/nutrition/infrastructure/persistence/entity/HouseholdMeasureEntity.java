package com.fuelmatch.nutrition.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Medida caseira associada a um alimento.
 * <p>
 * Exemplos:
 * <ul>
 *   <li>name="1 colher de sopa", quantity=1, weight_g=15</li>
 *   <li>name="1 xícara de chá", quantity=1, weight_g=160</li>
 *   <li>name="1 fatia média", quantity=1, weight_g=30</li>
 * </ul>
 * O campo {@code weight_g} representa o peso total em gramas da medida descrita.
 */
@Entity
@Table(name = "household_measures",
       uniqueConstraints = @UniqueConstraint(columnNames = {"food_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class HouseholdMeasureEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_id", nullable = false)
    private FoodEntity food;

    /**
     * Descrição da medida caseira. Ex: "1 colher de sopa", "1 xícara de chá".
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Quantidade de unidades da medida. Geralmente 1, mas pode ser fracionado.
     * Ex: quantity=0.5 para "meia xícara".
     */
    @Column(nullable = false, precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    /**
     * Peso em gramas correspondente a {@code quantity} unidades desta medida.
     */
    @Column(name = "weight_g", nullable = false, precision = 8, scale = 2)
    private BigDecimal weightG;

    /**
     * Indica se esta é a medida padrão exibida por default no frontend.
     * O trigger no banco garante unicidade por alimento.
     */
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean defaultMeasure = false;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}
