package com.fuelmatch.nutrition.infrastructure.integration.openfoodfacts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para resposta de busca textual da Open Food Facts.
 * {@code GET /cgi/search.pl}
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OffSearchResponse {

    private int count;
    private int page;

    @JsonProperty("page_size")
    private int pageSize;

    @JsonProperty("page_count")
    private int pageCount;

    private List<OffProductResponse.OffProduct> products;
}
