package com.fuelmatch.nutrition.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request para cálculo de macros em lote — essencial para a montagem de
 * planos alimentares completos no FuelMatch.
 *
 * <p>Permite calcular macros de até 50 alimentos em uma única requisição,
 * eliminando N chamadas individuais ao {@code POST /calculate}.
 *
 * <p>Caso de uso: usuário adiciona 8 alimentos à refeição do café da manhã;
 * o frontend faz 1 chamada batch em vez de 8 calls sequenciais.
 */
@Data
@NoArgsConstructor
public class BatchCalculateRequest {

    @NotEmpty(message = "Pelo menos um item é obrigatório")
    @Size(max = 50, message = "Máximo de 50 itens por batch")
    @Valid
    private List<BatchItem> items;

    @Data
    @NoArgsConstructor
    public static class BatchItem {

        @NotNull(message = "foodId é obrigatório")
        private UUID foodId;

        @NotNull(message = "portionInput é obrigatório")
        private String portionInput;

        /** Identificador livre para correlação no frontend (ex: id do item da refeição). */
        private String correlationId;
    }
}
