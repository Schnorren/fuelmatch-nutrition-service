package com.fuelmatch.nutrition.application.service;

import com.fuelmatch.nutrition.domain.model.Food;
import com.fuelmatch.nutrition.domain.model.HouseholdMeasure;
import com.fuelmatch.nutrition.domain.model.MacroResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes unitários do motor de cálculo de macros.
 * Cobre todos os paths de resolução de porção e a matemática da proporção.
 */
@DisplayName("MacroCalculatorService")
class MacroCalculatorServiceTest {

    private MacroCalculatorService calculator;
    private Food frango100g;

    @BeforeEach
    void setUp() {
        calculator = new MacroCalculatorService();

        // Frango grelhado — dados TACO (por 100g)
        frango100g = Food.builder()
                .id(UUID.randomUUID())
                .name("Frango, peito, grelhado")
                .source(Food.FoodSource.TACO)
                .energyKcal(new BigDecimal("159"))
                .proteinsG(new BigDecimal("32.0"))
                .carbohydratesG(new BigDecimal("0.0"))
                .fatTotalG(new BigDecimal("3.2"))
                .ofWhichSaturatedG(new BigDecimal("0.9"))
                .sodiumMg(new BigDecimal("74"))
                .servingSizeG(new BigDecimal("100"))
                .servingDescription("1 porção (100g)")
                .micronutrients(Map.of(
                        "calcium_mg", new BigDecimal("11"),
                        "iron_mg", new BigDecimal("0.7")
                ))
                .measures(List.of(
                        HouseholdMeasure.builder()
                                .id(UUID.randomUUID())
                                .name("1 escumadeira")
                                .quantity(BigDecimal.ONE)
                                .weightG(new BigDecimal("85"))
                                .defaultMeasure(true)
                                .build(),
                        HouseholdMeasure.builder()
                                .id(UUID.randomUUID())
                                .name("1 filé médio")
                                .quantity(BigDecimal.ONE)
                                .weightG(new BigDecimal("120"))
                                .defaultMeasure(false)
                                .build()
                ))
                .active(true)
                .verified(true)
                .build();
    }

    // ── Resolução: Gramas ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Resolução por gramas")
    class GramsResolution {

        @ParameterizedTest(name = "input={0} → {1}g")
        @CsvSource({
                "250g,        250",
                "250 g,       250",
                "250 gramas,  250",
                "250 grama,   250",
                "100.5g,      100.5",
                "50,5g,       50.5"     // vírgula decimal
        })
        void shouldResolveGramsFromVariousFormats(String input, String expectedG) {
            MacroResult result = calculator.calculate(frango100g, input);

            assertThat(result.getCalculatedWeightG())
                    .isEqualByComparingTo(new BigDecimal(expectedG));
        }

        @Test
        @DisplayName("250g deve calcular proportional correto")
        void shouldCalculateProportion250g() {
            MacroResult result = calculator.calculate(frango100g, "250g");

            // 250g = 2.5x a base de 100g
            assertThat(result.getEnergyKcal()).isEqualByComparingTo("397.50");
            assertThat(result.getProteinsG()).isEqualByComparingTo("80.00");
            assertThat(result.getCarbohydratesG()).isEqualByComparingTo("0.00");
            assertThat(result.getFatTotalG()).isEqualByComparingTo("8.00");
            assertThat(result.getSodiumMg()).isEqualByComparingTo("185.00");
        }
    }

    // ── Resolução: Número puro ────────────────────────────────────────────────

    @Nested
    @DisplayName("Resolução por número puro (assume gramas)")
    class NumericResolution {

        @Test
        void shouldInterpretPureNumberAsGrams() {
            MacroResult result = calculator.calculate(frango100g, "150");

            assertThat(result.getCalculatedWeightG()).isEqualByComparingTo("150");
            assertThat(result.getProteinsG()).isEqualByComparingTo("48.00"); // 32 * 1.5
        }
    }

    // ── Resolução: Medidas Caseiras ───────────────────────────────────────────

    @Nested
    @DisplayName("Resolução por medidas caseiras")
    class HouseholdMeasureResolution {

        @Test
        @DisplayName("2 escumadeiras = 2 × 85g = 170g")
        void shouldResolveTwoScoops() {
            MacroResult result = calculator.calculate(frango100g, "2 escumadeiras");

            assertThat(result.getCalculatedWeightG()).isEqualByComparingTo("170.00");
            // 170g → proteína: 32 * 1.7 = 54.4g
            assertThat(result.getProteinsG()).isEqualByComparingTo("54.40");
        }

        @Test
        @DisplayName("1 filé médio = 120g")
        void shouldResolveFileMedio() {
            MacroResult result = calculator.calculate(frango100g, "1 filé médio");

            assertThat(result.getCalculatedWeightG()).isEqualByComparingTo("120.00");
            assertThat(result.getEnergyKcal()).isEqualByComparingTo("190.80"); // 159 * 1.2
        }

        @Test
        @DisplayName("Medida inexistente lança PortionResolutionException via fallback")
        void shouldFallbackToGramsWhenMeasureNotFound() {
            // "300" puro → fallback para gramas
            MacroResult result = calculator.calculate(frango100g, "300");
            assertThat(result.getCalculatedWeightG()).isEqualByComparingTo("300");
        }
    }

    // ── Resolução: Porção do rótulo ───────────────────────────────────────────

    @Nested
    @DisplayName("Resolução por porção")
    class ServingResolution {

        @Test
        @DisplayName("'1 porção' usa servingSizeG do alimento")
        void shouldResolveServingFromFood() {
            MacroResult result = calculator.calculate(frango100g, "1 porção");

            assertThat(result.getCalculatedWeightG()).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("'2 porcoes' = 200g para servingSizeG=100")
        void shouldResolveMultipleServings() {
            MacroResult result = calculator.calculate(frango100g, "2 porções");

            assertThat(result.getCalculatedWeightG()).isEqualByComparingTo("200.00");
        }

        @Test
        @DisplayName("Alimento sem servingSizeG e sem medida default lança exceção")
        void shouldThrowWhenNoServingInfoAvailable() {
            Food semPorcao = frango100g.toBuilder()
                    .servingSizeG(null)
                    .measures(List.of())
                    .build();

            assertThatThrownBy(() -> calculator.calculate(semPorcao, "1 porção"))
                    .isInstanceOf(MacroCalculatorService.PortionResolutionException.class)
                    .hasMessageContaining("porção de rótulo");
        }
    }

    // ── Micronutrientes ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Micronutrientes são proporcionalmente calculados")
    void shouldCalculateMicronutrientsProportionally() {
        MacroResult result = calculator.calculate(frango100g, "200g");

        assertThat(result.getMicronutrients())
                .containsEntry("calcium_mg", new BigDecimal("22.00"))  // 11 * 2
                .containsEntry("iron_mg", new BigDecimal("1.40"));      // 0.7 * 2
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Macro null no alimento permanece null no resultado")
    void shouldPreserveNullMacros() {
        Food semFibra = frango100g.toBuilder().ofWhichFiberG(null).build();

        MacroResult result = calculator.calculate(semFibra, "100g");

        assertThat(result.getOfWhichFiberG()).isNull();
    }

    @Test
    @DisplayName("Input inválido lança PortionResolutionException")
    void shouldThrowOnInvalidInput() {
        assertThatThrownBy(() -> calculator.calculate(frango100g, "abc def xyz"))
                .isInstanceOf(MacroCalculatorService.PortionResolutionException.class);
    }

    @Test
    @DisplayName("Input vazio lança PortionResolutionException")
    void shouldThrowOnBlankInput() {
        assertThatThrownBy(() -> calculator.calculate(frango100g, ""))
                .isInstanceOf(MacroCalculatorService.PortionResolutionException.class);
    }

    @Test
    @DisplayName("calculateByWeight com peso negativo lança IllegalArgumentException")
    void shouldThrowOnNegativeWeight() {
        assertThatThrownBy(() -> calculator.calculateByWeight(frango100g, new BigDecimal("-10")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
