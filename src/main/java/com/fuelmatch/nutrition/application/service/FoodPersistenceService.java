package com.fuelmatch.nutrition.application.service;

import com.fuelmatch.nutrition.application.port.FoodRepository;
import com.fuelmatch.nutrition.domain.model.Food;
import com.fuelmatch.nutrition.infrastructure.persistence.entity.FoodEntity;
import com.fuelmatch.nutrition.infrastructure.persistence.entity.FoodImportLogEntity;
import com.fuelmatch.nutrition.infrastructure.persistence.repository.FoodImportLogJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Serviço responsável pela persistência assíncrona de alimentos
 * vindos da Open Food Facts (cache local) com auditoria via {@link FoodImportLogEntity}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FoodPersistenceService {

    private final FoodRepository foodRepository;
    private final FoodImportLogJpaRepository importLogRepository;

    /**
     * Persiste alimentos da Open Food Facts de forma assíncrona (fire-and-forget).
     * Cada item é processado individualmente: uma falha em um item não cancela os demais.
     */
    @Async("offPersistenceExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistOffFoodsAsync(List<Food> foods, String origin) {
        log.info("[ASYNC-PERSIST] Iniciando | origin='{}' | count={}", origin, foods.size());

        int saved = 0, skipped = 0, failed = 0;

        for (Food food : foods) {
            String externalId = food.getExternalId() != null
                    ? food.getExternalId() : food.getBarcode();
            try {
                if (isDuplicate(food)) {
                    importLogRepository.save(FoodImportLogEntity.duplicate(
                            FoodEntity.FoodSource.OPEN_FOOD_FACTS, externalId));
                    skipped++;
                    continue;
                }

                Food savedFood = foodRepository.save(food);
                importLogRepository.save(FoodImportLogEntity.success(
                        savedFood.getId(), FoodEntity.FoodSource.OPEN_FOOD_FACTS, externalId));
                saved++;
                log.debug("[ASYNC-PERSIST] Salvo: '{}' id={}", food.getName(), savedFood.getId());

            } catch (Exception e) {
                importLogRepository.save(FoodImportLogEntity.failed(
                        FoodEntity.FoodSource.OPEN_FOOD_FACTS, externalId, e.getMessage()));
                failed++;
                log.error("[ASYNC-PERSIST] Erro ao salvar '{}': {}", food.getName(), e.getMessage());
            }
        }

        log.info("[ASYNC-PERSIST] Concluído | origin='{}' | saved={} | skipped={} | failed={}",
                origin, saved, skipped, failed);
    }

    private boolean isDuplicate(Food food) {
        if (food.getBarcode() != null && !food.getBarcode().isBlank()) {
            if (foodRepository.existsByBarcode(food.getBarcode())) return true;
        }
        if (food.getExternalId() != null && !food.getExternalId().isBlank()) {
            return foodRepository.existsBySourceAndExternalId(
                    Food.FoodSource.OPEN_FOOD_FACTS, food.getExternalId());
        }
        return false;
    }
}
