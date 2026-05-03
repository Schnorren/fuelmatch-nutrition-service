package com.fuelmatch.nutrition.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fuelmatch.nutrition.api.dto.request.BatchCalculateRequest;
import com.fuelmatch.nutrition.api.dto.request.CalculateMacrosRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Testes de integração do FoodController usando banco PostgreSQL real via Testcontainers.
 *
 * <p>Valida:
 * <ul>
 *   <li>Fuzzy search no banco com pg_trgm</li>
 *   <li>Cálculo de macros com dados do seed TACO</li>
 *   <li>Cálculo batch com totais agregados</li>
 *   <li>Respostas 404 para IDs inexistentes</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@DisplayName("FoodController — Integration Tests")
class FoodControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("fuelmatch_nutrition_test")
            .withUsername("nutrition")
            .withPassword("nutrition")
            .withReuse(true);  // reutiliza container entre testes para velocidade

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    // UUID do "Frango, peito, grelhado" (seed V2)
    private static final String FRANGO_ID = "00000003-0000-0000-0000-000000000001";

    // ── Search Tests ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /search?q=frango deve retornar alimentos TACO seed")
    void searchFrangoReturnsResults() throws Exception {
        mockMvc.perform(get("/api/v1/foods/search")
                        .param("q", "frango")
                        .param("source", "TACO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.content[0].name",
                        containsStringIgnoringCase("frango")));
    }

    @Test
    @DisplayName("GET /search com erro de digitação (fuzzy) deve retornar resultado")
    void searchWithTypoStillReturnsResult() throws Exception {
        mockMvc.perform(get("/api/v1/foods/search")
                        .param("q", "frnago")  // "frango" com typo
                        .param("source", "TACO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("GET /search com termo inexistente retorna lista vazia")
    void searchUnknownTermReturnsEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/foods/search")
                        .param("q", "zzzzprodutoinexistentezzzz")
                        .param("source", "TACO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    // ── FindById Tests ────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /{id} com ID válido retorna alimento com medidas caseiras")
    void findByIdReturnsFoodWithMeasures() throws Exception {
        mockMvc.perform(get("/api/v1/foods/" + FRANGO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(FRANGO_ID))
                .andExpect(jsonPath("$.name").value("Frango, peito, grelhado"))
                .andExpect(jsonPath("$.source").value("TACO"))
                .andExpect(jsonPath("$.measures", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("GET /{id} com UUID inexistente retorna 404")
    void findByIdNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/foods/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // ── Calculate Tests ───────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /calculate com '250g' retorna macros proporcionais")
    void calculateFor250gReturnsCorrectMacros() throws Exception {
        CalculateMacrosRequest request = new CalculateMacrosRequest();
        request.setFoodId(UUID.fromString(FRANGO_ID));
        request.setPortionInput("250g");

        mockMvc.perform(post("/api/v1/foods/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calculatedWeightG").value(250.00))
                // 159 kcal / 100g × 250g = 397.50 kcal
                .andExpect(jsonPath("$.energyKcal").value(397.50))
                // 32g proteína / 100g × 250g = 80g
                .andExpect(jsonPath("$.proteinsG").value(80.00));
    }

    @Test
    @DisplayName("POST /calculate com medida caseira '1 filé médio'")
    void calculateByHouseholdMeasure() throws Exception {
        CalculateMacrosRequest request = new CalculateMacrosRequest();
        request.setFoodId(UUID.fromString(FRANGO_ID));
        request.setPortionInput("1 filé médio");  // 120g conforme seed

        mockMvc.perform(post("/api/v1/foods/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calculatedWeightG").value(120.00))
                .andExpect(jsonPath("$.proteinsG").value(38.40));  // 32 × 1.2
    }

    @Test
    @DisplayName("POST /calculate com porção inválida retorna 422")
    void calculateWithInvalidPortionReturns422() throws Exception {
        CalculateMacrosRequest request = new CalculateMacrosRequest();
        request.setFoodId(UUID.fromString(FRANGO_ID));
        request.setPortionInput("abc xyz inválido");

        mockMvc.perform(post("/api/v1/foods/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.hint").exists());
    }

    // ── Batch Calculate Tests ─────────────────────────────────────────────────

    @Test
    @DisplayName("POST /calculate/batch retorna totais agregados da refeição")
    void calculateBatchReturnsTotals() throws Exception {
        BatchCalculateRequest.BatchItem item1 = new BatchCalculateRequest.BatchItem();
        item1.setFoodId(UUID.fromString(FRANGO_ID));
        item1.setPortionInput("150g");
        item1.setCorrelationId("item-frango");

        // Arroz branco (seed TACO-001)
        BatchCalculateRequest.BatchItem item2 = new BatchCalculateRequest.BatchItem();
        item2.setFoodId(UUID.fromString("00000001-0000-0000-0000-000000000001"));
        item2.setPortionInput("100g");
        item2.setCorrelationId("item-arroz");

        BatchCalculateRequest request = new BatchCalculateRequest();
        request.setItems(List.of(item1, item2));

        mockMvc.perform(post("/api/v1/foods/calculate/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].success").value(true))
                .andExpect(jsonPath("$.items[0].correlationId").value("item-frango"))
                .andExpect(jsonPath("$.totals.totalEnergyKcal").isNumber())
                .andExpect(jsonPath("$.totals.totalProteinsG").isNumber())
                .andExpect(jsonPath("$.errorCount").value(0));
    }

    @Test
    @DisplayName("POST /calculate/batch com item inválido não cancela os demais")
    void calculateBatchWithOneErrorContinues() throws Exception {
        BatchCalculateRequest.BatchItem itemValido = new BatchCalculateRequest.BatchItem();
        itemValido.setFoodId(UUID.fromString(FRANGO_ID));
        itemValido.setPortionInput("100g");
        itemValido.setCorrelationId("valido");

        BatchCalculateRequest.BatchItem itemInvalido = new BatchCalculateRequest.BatchItem();
        itemInvalido.setFoodId(UUID.randomUUID()); // ID inexistente
        itemInvalido.setPortionInput("100g");
        itemInvalido.setCorrelationId("invalido");

        BatchCalculateRequest request = new BatchCalculateRequest();
        request.setItems(List.of(itemValido, itemInvalido));

        mockMvc.perform(post("/api/v1/foods/calculate/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].success").value(true))
                .andExpect(jsonPath("$.items[1].success").value(false))
                .andExpect(jsonPath("$.errorCount").value(1));
    }
}
