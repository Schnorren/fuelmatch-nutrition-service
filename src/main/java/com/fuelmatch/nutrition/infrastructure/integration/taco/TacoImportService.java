package com.fuelmatch.nutrition.infrastructure.integration.taco;

import com.fuelmatch.nutrition.application.port.FoodRepository;
import com.fuelmatch.nutrition.domain.model.Food;
import com.fuelmatch.nutrition.domain.model.HouseholdMeasure;
import com.fuelmatch.nutrition.infrastructure.persistence.entity.FoodEntity;
import com.fuelmatch.nutrition.infrastructure.persistence.entity.FoodImportLogEntity;
import com.fuelmatch.nutrition.infrastructure.persistence.repository.FoodImportLogJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Serviço de importação batch da Tabela TACO a partir de arquivo CSV.
 *
 * <p><b>Formato esperado do CSV</b> (com header):
 * <pre>
 * id,name,category,energy_kcal,energy_kj,carbs,fiber,sugars,protein,fat,saturated,sodium
 * 1,Arroz branco cozido,CEREAIS_GRAOS,128,537,28.1,1.6,,2.5,0.2,0.1,1
 * </pre>
 *
 * <p>O CSV da TACO completo (≈ 597 alimentos) pode ser obtido em:
 * {@code https://www.cfn.org.br/wp-content/uploads/2017/03/taco_4_edicao_ampliada_e_revisada.pdf}
 * e convertido para CSV via scripts de ETL.
 *
 * <p>Coloque o arquivo em {@code src/main/resources/taco/taco_4ed.csv}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TacoImportService {

    private static final String TACO_CSV_PATH = "/taco/taco_4ed.csv";
    private static final String TACO_SOURCE = "TACO";

    private final FoodRepository foodRepository;
    private final FoodImportLogJpaRepository importLogRepository;

    /**
     * Importa todos os alimentos do CSV da TACO.
     * Idempotente: alimentos já existentes (por external_id) são ignorados.
     *
     * @return estatísticas da importação
     */
    @Transactional
    public ImportResult importFromCsv() {
        log.info("Iniciando importação da Tabela TACO...");

        InputStream is = getClass().getResourceAsStream(TACO_CSV_PATH);
        if (is == null) {
            log.warn("Arquivo TACO não encontrado em {}. Pulando importação.", TACO_CSV_PATH);
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

                try {
                    String externalId = "TACO-" + cols[0].trim();

                    // Idempotência
                    if (foodRepository.existsBySourceAndExternalId(
                            Food.FoodSource.TACO, externalId)) {
                        skipped++;
                        continue;
                    }

                    Food food = buildFoodFromCsv(cols, externalId);
                    Food savedFood = foodRepository.save(food);

                    importLogRepository.save(FoodImportLogEntity.success(
                            savedFood.getId(),
                            FoodEntity.FoodSource.TACO,
                            externalId));
                    saved++;

                } catch (Exception e) {
                    failed++;
                    log.warn("Erro ao importar linha TACO '{}': {}", line, e.getMessage());
                    importLogRepository.save(FoodImportLogEntity.failed(
                            FoodEntity.FoodSource.TACO,
                            cols.length > 0 ? "TACO-" + cols[0] : "unknown",
                            e.getMessage()));
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

    // ── CSV Parsing ───────────────────────────────────────────────────────────

    private Food buildFoodFromCsv(String[] cols, String externalId) {
        // Formato: id,name,category,energy_kcal,energy_kj,carbs,fiber,sugars,protein,fat,saturated,sodium
        Food.FoodCategory category = parseCategory(safeGet(cols, 2));

        return Food.builder()
                .name(safeGet(cols, 1).trim())
                .source(Food.FoodSource.TACO)
                .externalId(externalId)
                .category(category)
                .energyKcal(parseBd(safeGet(cols, 3)))
                .energyKj(parseBd(safeGet(cols, 4)))
                .carbohydratesG(parseBd(safeGet(cols, 5)))
                .ofWhichFiberG(parseBd(safeGet(cols, 6)))
                .ofWhichSugarsG(parseBd(safeGet(cols, 7)))
                .proteinsG(parseBd(safeGet(cols, 8)))
                .fatTotalG(parseBd(safeGet(cols, 9)))
                .ofWhichSaturatedG(parseBd(safeGet(cols, 10)))
                .sodiumMg(parseBd(safeGet(cols, 11)))
                .micronutrients(Map.of())
                .active(true)
                .verified(true)   // TACO é fonte verificada
                .measures(List.of())
                .build();
    }

    /**
     * Parser CSV simples — suporta campos entre aspas com vírgulas internas.
     */
    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }

    private BigDecimal parseBd(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("NA")
                || value.equalsIgnoreCase("*") || value.equals("-")) {
            return null;
        }
        try {
            return new BigDecimal(value.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String safeGet(String[] cols, int index) {
        return (index < cols.length) ? cols[index] : "";
    }

    private Food.FoodCategory parseCategory(String raw) {
        if (raw == null || raw.isBlank()) return Food.FoodCategory.OUTROS;
        try {
            return Food.FoodCategory.valueOf(
                    raw.trim().toUpperCase()
                       .replace(' ', '_')
                       .replace('-', '_'));
        } catch (IllegalArgumentException e) {
            return Food.FoodCategory.OUTROS;
        }
    }

    // ── Result record ─────────────────────────────────────────────────────────

    public record ImportResult(int total, int saved, int skipped, int failed) {
        static ImportResult empty() {
            return new ImportResult(0, 0, 0, 0);
        }

        @Override
        public String toString() {
            return String.format("ImportResult{total=%d, saved=%d, skipped=%d, failed=%d}",
                    total, saved, skipped, failed);
        }
    }
}
