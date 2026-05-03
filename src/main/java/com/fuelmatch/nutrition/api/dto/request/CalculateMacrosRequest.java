package com.fuelmatch.nutrition.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request para cálculo de macros de um alimento.
 *
 * <p>O campo {@code portionInput} aceita formatos livres:
 * <ul>
 *   <li>{@code "250g"} — gramas explícitas</li>
 *   <li>{@code "2 colheres de sopa"} — medida caseira</li>
 *   <li>{@code "1 porção"} — porção do rótulo</li>
 *   <li>{@code "150"} — número puro (interpretado como gramas)</li>
 * </ul>
 */
@Data
@NoArgsConstructor
public class CalculateMacrosRequest {

    @NotNull(message = "foodId é obrigatório")
    private UUID foodId;

    @NotBlank(message = "portionInput é obrigatório")
    private String portionInput;
}
