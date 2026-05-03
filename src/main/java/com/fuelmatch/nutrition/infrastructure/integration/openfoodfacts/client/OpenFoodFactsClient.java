package com.fuelmatch.nutrition.infrastructure.integration.openfoodfacts.client;

import com.fuelmatch.nutrition.infrastructure.integration.openfoodfacts.dto.OffProductResponse;
import com.fuelmatch.nutrition.infrastructure.integration.openfoodfacts.dto.OffSearchResponse;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Optional;

/**
 * Cliente HTTP reativo para a API da Open Food Facts.
 *
 * <p>Usa {@link WebClient} com:
 * <ul>
 *   <li>Timeout de leitura: 5s</li>
 *   <li>Retry: 2 tentativas com backoff exponencial (1s → 2s)</li>
 *   <li>Graceful degradation: retorna {@link Optional#empty()} em caso de falha</li>
 * </ul>
 *
 * <p><b>Rate Limiting:</b> A Open Food Facts não exige autenticação mas solicita
 * identificação via User-Agent. Limite unofficial: ~100 req/min.
 */
@Component
@Slf4j
public class OpenFoodFactsClient {

    private static final String USER_AGENT = "FuelMatch-NutritionService/1.0 (contact@fuelmatch.com)";
    private static final int MAX_RETRY_ATTEMPTS = 2;
    private static final Duration RETRY_INITIAL_BACKOFF = Duration.ofSeconds(1);

    private final WebClient webClient;

    public OpenFoodFactsClient(
            WebClient.Builder webClientBuilder,
            @Value("${integration.open-food-facts.base-url:https://world.openfoodfacts.org}") String baseUrl,
            @Value("${integration.open-food-facts.timeout-seconds:5}") int timeoutSeconds) {

        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)) // 2MB
                .build();

        log.info("OpenFoodFactsClient inicializado. baseUrl={}, timeout={}s",
                baseUrl, timeoutSeconds);
    }

    // ── API: Busca por Barcode ────────────────────────────────────────────────

    /**
     * Busca um produto pelo código de barras (EAN-13 / EAN-8).
     * Retorna empty se não encontrado ou em caso de erro.
     */
    @Timed(value = "off.barcode_lookup", description = "OFF barcode lookup latency")
    public Optional<OffProductResponse> findByBarcode(String barcode) {
        log.debug("OFF barcode lookup: {}", barcode);
        try {
            OffProductResponse response = webClient.get()
                    .uri("/api/v2/product/{barcode}?fields={fields}", barcode, OFF_FIELDS)
                    .retrieve()
                    .bodyToMono(OffProductResponse.class)
                    .retryWhen(retrySpec())
                    .timeout(Duration.ofSeconds(5))
                    .block();

            if (response == null || response.getStatus() == 0) {
                log.debug("Produto não encontrado na OFF: barcode={}", barcode);
                return Optional.empty();
            }
            return Optional.of(response);
        } catch (Exception e) {
            log.warn("Falha ao buscar barcode {} na OFF: {}", barcode, e.getMessage());
            return Optional.empty();
        }
    }

    // ── API: Busca por Nome ───────────────────────────────────────────────────

    /**
     * Busca produtos por nome/termo na Open Food Facts.
     * Retorna empty em caso de falha (graceful degradation).
     */
    @Timed(value = "off.search", description = "OFF text search latency")
    public Optional<OffSearchResponse> searchByName(String query, int page, int pageSize) {
        log.debug("OFF search: query='{}', page={}", query, page);
        try {
            OffSearchResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/cgi/search.pl")
                            .queryParam("search_terms", query)
                            .queryParam("search_simple", 1)
                            .queryParam("action", "process")
                            .queryParam("json", 1)
                            .queryParam("page", page)
                            .queryParam("page_size", pageSize)
                            .queryParam("fields", OFF_FIELDS)
                            .queryParam("lc", "pt")            // prioriza PT
                            .build())
                    .retrieve()
                    .bodyToMono(OffSearchResponse.class)
                    .retryWhen(retrySpec())
                    .timeout(Duration.ofSeconds(5))
                    .block();

            return Optional.ofNullable(response);
        } catch (Exception e) {
            log.warn("Falha no search OFF para '{}': {}", query, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Versão reativa (non-blocking) da busca por nome.
     * Usada em flows assíncronos (background cache warming).
     */
    public Mono<OffSearchResponse> searchByNameReactive(String query, int pageSize) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/cgi/search.pl")
                        .queryParam("search_terms", query)
                        .queryParam("search_simple", 1)
                        .queryParam("action", "process")
                        .queryParam("json", 1)
                        .queryParam("page_size", pageSize)
                        .queryParam("fields", OFF_FIELDS)
                        .build())
                .retrieve()
                .bodyToMono(OffSearchResponse.class)
                .retryWhen(retrySpec())
                .timeout(Duration.ofSeconds(8))
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.warn("OFF search reactive error: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Retry retrySpec() {
        return Retry.backoff(MAX_RETRY_ATTEMPTS, RETRY_INITIAL_BACKOFF)
                .filter(throwable -> !(throwable instanceof WebClientResponseException.NotFound))
                .doBeforeRetry(signal -> log.debug(
                        "Retry #{} para OFF request: {}",
                        signal.totalRetriesInARow() + 1,
                        signal.failure().getMessage()));
    }

    /**
     * Campos solicitados à API OFF — minimiza payload e latência.
     */
    private static final String OFF_FIELDS = String.join(",",
            "code", "product_name", "brands", "categories_tags",
            "nutriments", "serving_size", "serving_quantity",
            "ingredients_text_pt", "ingredients_text",
            "allergens_tags", "image_front_url",
            "nutriment_energy-kcal_100g",
            "nutriment_proteins_100g", "nutriment_carbohydrates_100g",
            "nutriment_fat_100g", "nutriment_fiber_100g",
            "nutriment_sugars_100g", "nutriment_saturated-fat_100g",
            "nutriment_sodium_100g"
    );
}
