package com.fuelmatch.nutrition.application.port;

import com.fuelmatch.nutrition.domain.model.Food;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Port de saída (Output Port) para persistência de alimentos.
 *
 * <p>Define o contrato que a camada de aplicação espera.
 * A implementação concreta está em
 * {@link com.fuelmatch.nutrition.infrastructure.persistence.adapter.FoodRepositoryAdapter}.
 *
 * <p>Este pattern (Hexagonal / Ports & Adapters) permite:
 * <ul>
 *   <li>Trocar a implementação (JPA → outro) sem tocar na lógica de negócio.</li>
 *   <li>Mockar facilmente nos testes unitários dos services.</li>
 * </ul>
 */
public interface FoodRepository {

    Page<Food> fuzzySearch(String query, Food.FoodSource source,
                           UUID tenantId, Pageable pageable);

    Optional<Food> findById(UUID id);

    Optional<Food> findByIdWithMeasures(UUID id);

    Optional<Food> findByBarcode(String barcode);

    Optional<Food> findBySourceAndExternalId(Food.FoodSource source, String externalId);

    boolean existsByBarcode(String barcode);

    boolean existsBySourceAndExternalId(Food.FoodSource source, String externalId);

    Food save(Food food);

    void deactivate(UUID id);
}
