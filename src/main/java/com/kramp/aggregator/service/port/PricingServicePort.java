package com.kramp.aggregator.service.port;

import com.kramp.aggregator.model.upstream.PricingData;

/** Port for the Pricing upstream service. customerId may be null for anonymous requests. */
public interface PricingServicePort {
    PricingData fetch(String productId, String market, String customerId);
}
