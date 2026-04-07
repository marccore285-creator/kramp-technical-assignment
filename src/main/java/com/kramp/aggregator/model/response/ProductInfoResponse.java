package com.kramp.aggregator.model.response;

import lombok.Builder;
import lombok.Value;

/**
 * Aggregated product response. Catalog is always populated; other sections may be degraded.
 */
@Value
@Builder
public class ProductInfoResponse {

    String productId;
    String market;
    CatalogInfo catalog;
    PricingInfo pricing;
    AvailabilityInfo availability;
    CustomerInfo customer;
}
