package com.kramp.aggregator.model.upstream;

/**
 * Raw data returned by the upstream Pricing service.
 * Currency is resolved separately via MarketUtils — the pricing service
 * deals in amounts only, market-to-currency mapping is our responsibility.
 */
public record PricingData(
        double basePrice,
        double discount,
        double finalPrice
) {}
