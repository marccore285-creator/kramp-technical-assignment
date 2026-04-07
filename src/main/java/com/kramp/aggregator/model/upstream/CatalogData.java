package com.kramp.aggregator.model.upstream;

import java.util.List;
import java.util.Map;

/**
 * Raw data returned by the upstream Catalog service.
 * Intentionally a plain record — no presentation concerns here.
 */
public record CatalogData(
        String productId,
        String name,
        String description,
        Map<String, String> specs,
        List<String> images
) {}
