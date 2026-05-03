package com.fuelmatch.nutrition.application.service;

import com.fuelmatch.nutrition.domain.model.Food;
import com.fuelmatch.nutrition.domain.model.HouseholdMeasure;
import com.fuelmatch.nutrition.domain.model.MacroResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Motor de cálculo de macronutrientes do FuelMatch.
 *
 * <p>Responsabilidades:
 * <ol>
 *   <li>Resolver a porção do usuário (ex: "250g", "2 colheres de sopa", "1 porção")
 *       para um valor em <b>gramas</b>.</li>
 *   <li>Aplicar regra de três sobre os macros do alimento (base 100g).</li>
 *   <li>Retornar um {@link MacroResult} com todos os macros calculados.</li>
 * </ol>
 *
 * <h3>Lógica de resolução de porção (em ordem de prioridade):</h3>
 * <ol>
 *   <li>Input em gramas explícito: {@code "250g"}, {@code "250 g"}, {@code "250 gramas"}</li>
 *   <li>Medida caseira por nome: {@code "2 colheres de sopa"}, {@code "1 xícara"}</li>
 *   <li>Porção do rótulo: {@code "1 porção"}, {@code "1 serving"}</li>
 *   <li>Número puro (assume gramas): {@code "150"}</li>
 * </ol>
 */
@Service
@Slf4j
public class MacroCalculatorService {

    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int RESULT_SCALE = 2;

    /** Patterns para resolver o input textual do usuário */
    private static final Pattern GRAMS_PATTERN =
            Pattern.compile("^\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:g|gramas?|grams?)\\s*$",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern NUMERIC_ONLY_PATTERN =
            Pattern.compile("^\\s*(\\d+(?:[.,]\\d+)?)\\s*$");

    private static final Pattern QUANTITY_MEASURE_PATTERN =
            Pattern.compile("^\\s*(\\d+(?:[.,]\\d+)?)\\s+(.+)$",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern SERVING_PATTERN =
            Pattern.compile("^\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:porç[aã]o|porcao|serving|serv)\\s*$",
                    Pattern.CASE_INSENSITIVE);

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Calcula macros para um alimento com base em um input textual do usuário.
     *
     * @param food         alimento com macros em base 100g
     * @param portionInput input livre do usuário: "250g", "2 colheres de sopa", "1 porção"
     * @return resultado com todos os macros proporcionais
     * @throws PortionResolutionException se o input não puder ser interpretado
     */
    public MacroResult calculate(Food food, String portionInput) {
        BigDecimal weightG = resolveWeightG(food, portionInput);
        return buildResult(food, weightG, portionInput);
    }

    /**
     * Calcula macros para um peso em gramas explícito.
     * Útil quando a UI já fornece o valor numérico diretamente.
     *
     * @param food    alimento com macros em base 100g
     * @param weightG quantidade em gramas
     * @return resultado com todos os macros proporcionais
     */
    public MacroResult calculateByWeight(Food food, BigDecimal weightG) {
        if (weightG == null || weightG.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "weightG deve ser positivo, recebido: " + weightG);
        }
        return buildResult(food, weightG, weightG.toPlainString() + "g");
    }

    /**
     * Calcula macros para uma medida caseira específica + multiplicador.
     *
     * @param food         alimento
     * @param measureName  nome da medida caseira (ex: "colher de sopa")
     * @param multiplier   quantidade de medidas (ex: 2.0 para "2 colheres de sopa")
     * @return resultado calculado
     * @throws PortionResolutionException se a medida não for encontrada
     */
    public MacroResult calculateByMeasure(Food food, String measureName,
                                          BigDecimal multiplier) {
        HouseholdMeasure measure = findMeasure(food.getMeasures(), measureName)
                .orElseThrow(() -> new PortionResolutionException(
                        "Medida caseira não encontrada: '" + measureName +
                        "' para o alimento: " + food.getName()));

        BigDecimal weightG = measure.getWeightG()
                .multiply(multiplier, MC)
                .divide(measure.getQuantity(), MC);

        String desc = multiplier.toPlainString() + " " + measureName;
        return buildResult(food, weightG, desc);
    }

    // ── Resolution Logic ──────────────────────────────────────────────────────

    /**
     * Resolve o input textual para um peso em gramas.
     * Percorre os resolvers em ordem de especificidade.
     */
    BigDecimal resolveWeightG(Food food, String input) {
        if (input == null || input.isBlank()) {
            throw new PortionResolutionException("Input de porção não pode ser vazio.");
        }

        String trimmed = input.trim();

        // 1. Gramas explícitos: "250g", "250 g", "250 gramas"
        Matcher gramsMatcher = GRAMS_PATTERN.matcher(trimmed);
        if (gramsMatcher.matches()) {
            return parseDecimal(gramsMatcher.group(1));
        }

        // 2. Porção do rótulo: "1 porção", "2 servings"
        Matcher servingMatcher = SERVING_PATTERN.matcher(trimmed);
        if (servingMatcher.matches()) {
            BigDecimal qty = parseDecimal(servingMatcher.group(1));
            return resolveServingWeight(food, qty);
        }

        // 3. Número + medida caseira: "2 colheres de sopa", "1 xícara"
        Matcher qtyMeasureMatcher = QUANTITY_MEASURE_PATTERN.matcher(trimmed);
        if (qtyMeasureMatcher.matches()) {
            BigDecimal qty = parseDecimal(qtyMeasureMatcher.group(1));
            String measureName = qtyMeasureMatcher.group(2).trim();
            Optional<HouseholdMeasure> measure = findMeasure(food.getMeasures(), measureName);
            if (measure.isPresent()) {
                return measure.get().getWeightG()
                        .multiply(qty, MC)
                        .divide(measure.get().getQuantity(), MC);
            }
        }

        // 4. Número puro → assume gramas
        Matcher numericMatcher = NUMERIC_ONLY_PATTERN.matcher(trimmed);
        if (numericMatcher.matches()) {
            log.debug("Input numérico puro '{}' interpretado como gramas.", trimmed);
            return parseDecimal(numericMatcher.group(1));
        }

        throw new PortionResolutionException(
                "Não foi possível interpretar a porção: '" + input + "'. " +
                "Use formatos como: '250g', '2 colheres de sopa', '1 porção'.");
    }

    private BigDecimal resolveServingWeight(Food food, BigDecimal quantity) {
        // Usa servingSizeG do alimento se disponível
        if (food.getServingSizeG() != null && food.getServingSizeG().compareTo(BigDecimal.ZERO) > 0) {
            return food.getServingSizeG().multiply(quantity, MC);
        }
        // Fallback: usa a medida default
        if (food.getMeasures() != null) {
            Optional<HouseholdMeasure> defaultMeasure = food.getMeasures().stream()
                    .filter(HouseholdMeasure::isDefaultMeasure)
                    .findFirst();
            if (defaultMeasure.isPresent()) {
                return defaultMeasure.get().getWeightG()
                        .multiply(quantity, MC)
                        .divide(defaultMeasure.get().getQuantity(), MC);
            }
        }
        throw new PortionResolutionException(
                "Alimento '" + food.getName() + "' não possui porção de rótulo definida. " +
                "Use gramas ou uma medida caseira específica.");
    }

    // ── Calculation Core ──────────────────────────────────────────────────────

    /**
     * Aplica regra de três: {@code macro_resultado = (macro_por_100g * peso_g) / 100}.
     */
    private MacroResult buildResult(Food food, BigDecimal weightG, String portionDesc) {
        log.debug("Calculando macros: food={}, weight={}g", food.getName(), weightG);

        return MacroResult.builder()
                .foodId(food.getId())
                .foodName(food.getName())
                .brand(food.getBrand())
                .calculatedWeightG(weightG.setScale(RESULT_SCALE, RoundingMode.HALF_UP))
                .portionDescription(portionDesc)
                .energyKcal(proportion(food.getEnergyKcal(), weightG))
                .energyKj(proportion(food.getEnergyKj(), weightG))
                .carbohydratesG(proportion(food.getCarbohydratesG(), weightG))
                .ofWhichSugarsG(proportion(food.getOfWhichSugarsG(), weightG))
                .ofWhichFiberG(proportion(food.getOfWhichFiberG(), weightG))
                .proteinsG(proportion(food.getProteinsG(), weightG))
                .fatTotalG(proportion(food.getFatTotalG(), weightG))
                .ofWhichSaturatedG(proportion(food.getOfWhichSaturatedG(), weightG))
                .ofWhichTransG(proportion(food.getOfWhichTransG(), weightG))
                .sodiumMg(proportion(food.getSodiumMg(), weightG))
                .micronutrients(proportionMap(food.getMicronutrients(), weightG))
                .build();
    }

    /**
     * Regra de três central: {@code (value / 100) * weightG}.
     * Retorna {@code null} se o macro não estiver disponível para o alimento.
     */
    private BigDecimal proportion(BigDecimal valuePer100g, BigDecimal weightG) {
        if (valuePer100g == null) return null;
        return valuePer100g
                .multiply(weightG, MC)
                .divide(HUNDRED, MC)
                .setScale(RESULT_SCALE, RoundingMode.HALF_UP);
    }

    private Map<String, BigDecimal> proportionMap(Map<String, BigDecimal> micronutrients,
                                                   BigDecimal weightG) {
        if (micronutrients == null || micronutrients.isEmpty()) return Map.of();
        Map<String, BigDecimal> result = new HashMap<>();
        micronutrients.forEach((key, value) ->
                result.put(key, proportion(value, weightG)));
        return Map.copyOf(result);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Busca uma medida caseira pelo nome (fuzzy: ignora case e espaços extras).
     */
    private Optional<HouseholdMeasure> findMeasure(List<HouseholdMeasure> measures,
                                                    String name) {
        if (measures == null || name == null) return Optional.empty();
        String normalized = normalizeString(name);
        return measures.stream()
                .filter(m -> normalizeString(m.getName()).equals(normalized)
                          || normalizeString(m.getName()).contains(normalized)
                          || normalized.contains(normalizeString(m.getName())))
                .findFirst();
    }

    private String normalizeString(String s) {
        return s.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    private BigDecimal parseDecimal(String value) {
        return new BigDecimal(value.replace(',', '.'));
    }

    // ── Exception ─────────────────────────────────────────────────────────────

    public static class PortionResolutionException extends RuntimeException {
        public PortionResolutionException(String message) {
            super(message);
        }
    }
}
