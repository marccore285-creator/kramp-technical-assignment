package com.kramp.aggregator.service.port;

import com.kramp.aggregator.model.upstream.AvailabilityData;

/** Port for the Availability upstream service. */
public interface AvailabilityServicePort {
    AvailabilityData fetch(String productId, String market);
}
