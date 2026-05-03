package com.fuelmatch.nutrition.domain.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Medida caseira de um alimento.
 * {@code weightG} é o peso total em gramas desta medida.
 */
@Value
@Builder
public class HouseholdMeasure {
    UUID id;
    String name;
    BigDecimal quantity;
    BigDecimal weightG;
    boolean defaultMeasure;
}
