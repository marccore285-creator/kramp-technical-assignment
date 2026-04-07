package com.kramp.aggregator.service.port;

import com.kramp.aggregator.model.upstream.CatalogData;

/**
 * Port (interface) for the Catalog upstream service.
 * AggregationService depends on this interface, not the mock implementation,
 * which makes it trivial to swap in a real HTTP/gRPC client later.
 */
public interface CatalogServicePort {
    CatalogData fetch(String productId, String market);
}
