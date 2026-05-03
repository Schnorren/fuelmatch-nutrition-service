package com.fuelmatch.nutrition.api.controller;

import com.fuelmatch.nutrition.infrastructure.integration.taco.TacoImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

/**
 * Controller de operações administrativas.
 *
 * <p>Todos os endpoints sob {@code /api/v1/admin} devem ser protegidos
 * por autenticação (ex: Spring Security com role ADMIN) no ambiente de produção.
 *
 * <p><b>Importante:</b> adicionar {@code @PreAuthorize("hasRole('ADMIN')")} após
 * integrar Spring Security.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
public class FoodAdminController {

    private final TacoImportService tacoImportService;
    private final CacheManager cacheManager;

    /**
     * Dispara importação batch da Tabela TACO a partir do CSV interno.
     * Idempotente — executa apenas para alimentos ainda não importados.
     *
     * <p>POST {@code /api/v1/admin/import/taco}
     */
    @PostMapping("/import/taco")
    public ResponseEntity<Map<String, Object>> importTaco() {
        log.info("Importação TACO disparada via admin endpoint.");
        TacoImportService.ImportResult result = tacoImportService.importFromCsv();
        return ResponseEntity.ok(Map.of(
                "status", "completed",
                "total",   result.total(),
                "saved",   result.saved(),
                "skipped", result.skipped(),
                "failed",  result.failed()
        ));
    }

    /**
     * Invalida todos os caches Redis do serviço.
     *
     * <p>POST {@code /api/v1/admin/cache/evict}
     */
    @PostMapping("/cache/evict")
    public ResponseEntity<Map<String, Object>> evictAllCaches() {
        cacheManager.getCacheNames().forEach(cacheName ->
                Objects.requireNonNull(cacheManager.getCache(cacheName)).clear());

        log.info("Cache evict executado para todos os caches: {}",
                cacheManager.getCacheNames());

        return ResponseEntity.ok(Map.of(
                "status", "evicted",
                "caches", cacheManager.getCacheNames()
        ));
    }

    /**
     * Invalida o cache de um alimento específico pelo ID.
     *
     * <p>DELETE {@code /api/v1/admin/cache/food/{id}}
     */
    @DeleteMapping("/cache/food/{id}")
    public ResponseEntity<Void> evictFoodCache(@PathVariable String id) {
        var detailCache = cacheManager.getCache("food-detail");
        var barcodeCache = cacheManager.getCache("food-barcode");

        if (detailCache != null) detailCache.evict(id);
        if (barcodeCache != null) barcodeCache.evict(id);

        log.debug("Cache evict para food id={}", id);
        return ResponseEntity.noContent().build();
    }
}
