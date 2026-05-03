package com.fuelmatch.nutrition.infrastructure.integration.taco;

import com.fuelmatch.nutrition.application.port.FoodRepository;
import com.fuelmatch.nutrition.domain.model.Food;
import com.fuelmatch.nutrition.infrastructure.persistence.entity.FoodEntity;
import com.fuelmatch.nutrition.infrastructure.persistence.entity.FoodImportLogEntity;
import com.fuelmatch.nutrition.infrastructure.persistence.repository.FoodImportLogJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Serviço de importação completa da Tabela TACO 4ª Edição.
 *
 * <p>Espera o CSV limpo gerado pelo script {@code convert_taco_csv.py} em:
 * {@code src/main/resources/taco/taco_4ed.csv}
 *
 * <p>Formato do CSV (com header):
 * <pre>
 * taco_id,name,category,energy_kcal,energy_kj,proteins_g,fat_total_g,
 * carbohydrates_g,fiber_g,cholesterol_mg,ash_g,calcium_mg,magnesium_mg,
 * manganese_mg,phosphorus_mg,iron_mg,sodium_mg,potassium_mg,copper_mg,
 * zinc_mg,retinol_mcg,re_mcg,rae_mcg,thiamine_mg,riboflavin_mg,
 * pyridoxine_mg,niacin_mg,vitamin_c_mg
 * </pre>
 *
 * <p>Micronutrientes são armazenados no campo JSONB {@code micronutrients}
 * da entidade {@link com.fuelmatch.nutrition.infrastructure.persistence.entity.FoodEntity}.
 * Macros principais ficam nas colunas dedicadas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TacoImportService {

    private static final String TACO_CSV_PATH = "/taco/taco_4ed.csv";

    private final FoodRepository foodRepository;
    private final FoodImportLogJpaRepository importLogRepository;

    /**
     * Importa todos os alimentos do CSV da TACO.
     * Operação idempotente — alimentos já existentes são ignorados.
     *
     * @return estatísticas da importação
     */
    @Transactional
    public ImportResult importFromCsv() {
        log.info("Iniciando importação completa da Tabela TACO...");

        InputStream is = getClass().getResourceAsStream(TACO_CSV_PATH);
        if (is == null) {
            log.warn("Arquivo TACO não encontrado em {}.", TACO_CSV_PATH);
            log.warn("Coloque o arquivo em src/main/resources/taco/taco_4ed.csv");
            return ImportResult.empty();
        }

        int total = 0, saved = 0, skipped = 0, failed = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String header = reader.readLine(); // descarta header
            log.debug("Header TACO: {}", header);

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                total++;

                String[] cols = parseCsvLine(line);
                if (cols.length < 28) {
                    log.warn("Linha com colunas insuficientes ({}): {}", cols.length, line);
                    failed++;
                    continue;
                }

                String externalId = "TACO-" + cols[0].trim();

                try {
                    if (foodRepository.existsBySourceAndExternalId(
                            Food.FoodSource.TACO, externalId)) {
                        skipped++;
                        continue;
                    }

                    Food food = buildFood(cols, externalId);
                    Food savedFood = foodRepository.save(food);

                    importLogRepository.save(FoodImportLogEntity.success(
                            savedFood.getId(),
                            FoodEntity.FoodSource.TACO,
                            externalId));
                    saved++;

                    if (saved % 50 == 0) {
                        log.info("TACO import: {}/{} processados...", saved + skipped, total);
                    }

                } catch (Exception e) {
                    failed++;
                    log.warn("Erro ao importar '{}' ({}): {}", externalId, cols[1], e.getMessage());
                    importLogRepository.save(FoodImportLogEntity.failed(
                            FoodEntity.FoodSource.TACO, externalId, e.getMessage()));
                }
            }
        } catch (IOException e) {
            log.error("Erro ao ler CSV TACO: {}", e.getMessage(), e);
            throw new RuntimeException("Falha na leitura do arquivo TACO CSV", e);
        }

        ImportResult result = new ImportResult(total, saved, skipped, failed);
        log.info("Importação TACO concluída: {}", result);
        return result;
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    private Food buildFood(String[] c, String externalId) {
        // Macros principais (colunas dedicadas)
        Food.FoodCategory category = parseCategory(c[2]);

        // Micronutrientes → JSONB
        Map<String, BigDecimal> micros = new LinkedHashMap<>();
        putMicro(micros, "cholesterol_mg",   c[9]);
        putMicro(micros, "ash_g",            c[10]);
        putMicro(micros, "calcium_mg",       c[11]);
        putMicro(micros, "magnesium_mg",     c[12]);
        putMicro(micros, "manganese_mg",     c[13]);
        putMicro(micros, "phosphorus_mg",    c[14]);
        putMicro(micros, "iron_mg",          c[15]);
        putMicro(micros, "potassium_mg",     c[17]);
        putMicro(micros, "copper_mg",        c[18]);
        putMicro(micros, "zinc_mg",          c[19]);
        putMicro(micros, "retinol_mcg",      c[20]);
        putMicro(micros, "re_mcg",           c[21]);
        putMicro(micros, "rae_mcg",          c[22]);
        putMicro(micros, "thiamine_mg",      c[23]);
        putMicro(micros, "riboflavin_mg",    c[24]);
        putMicro(micros, "pyridoxine_mg",    c[25]);
        putMicro(micros, "niacin_mg",        c[26]);
        putMicro(micros, "vitamin_c_mg",     c[27]);

        return Food.builder()
                .name(c[1].trim())
                .source(Food.FoodSource.TACO)
                .externalId(externalId)
                .category(category)
                // Macros por 100g
                .energyKcal(parseBd(c[3]))
                .energyKj(parseBd(c[4]))
                .proteinsG(parseBd(c[5]))
                .fatTotalG(parseBd(c[6]))
                .carbohydratesG(parseBd(c[7]))
                .ofWhichFiberG(parseBd(c[8]))
                // Sódio como coluna dedicada (relevante para dietas)
                .sodiumMg(parseBd(c[16]))
                // Micronutrientes completos no JSONB
                .micronutrients(micros)
                .allergens(new String[0])
                .active(true)
                .verified(true)
                .measures(List.of())
                .build();
    }

    private void putMicro(Map<String, BigDecimal> map, String key, String val) {
        BigDecimal bd = parseBd(val);
        if (bd != null) map.put(key, bd);
    }

    // ── Parsers ───────────────────────────────────────────────────────────────

    /**
     * Parser CSV RFC-4180 simples — suporta campos entre aspas com vírgulas.
     */
    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

    private BigDecimal parseBd(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty() || v.equalsIgnoreCase("NA") || v.equals("*")
                || v.equals("-") || v.equalsIgnoreCase("Tr")
                || v.equalsIgnoreCase("Tr.") || v.equalsIgnoreCase("ND")) {
            return null;
        }
        try {
            return new BigDecimal(v.replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Food.FoodCategory parseCategory(String raw) {
        if (raw == null || raw.isBlank()) return Food.FoodCategory.OUTROS;
        try {
            return Food.FoodCategory.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Food.FoodCategory.OUTROS;
        }
    }

    // ── Result ────────────────────────────────────────────────────────────────

    public record ImportResult(int total, int saved, int skipped, int failed) {
        static ImportResult empty() { return new ImportResult(0, 0, 0, 0); }

        @Override
        public String toString() {
            return String.format(
                "ImportResult{total=%d, saved=%d, skipped=%d, failed=%d}",
                total, saved, skipped, failed);
        }
    }
}  