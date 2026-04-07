package com.kramp.aggregator.model.upstream;

/** Raw data returned by the upstream Availability service. */
public record AvailabilityData(
        int stockLevel,
        String warehouse,
        int expectedDeliveryDays
) {}
