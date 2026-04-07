package com.kramp.aggregator.service.port;

import com.kramp.aggregator.model.upstream.CustomerData;

/** Port for the Customer upstream service. Only called when a customerId is present. */
public interface CustomerServicePort {
    CustomerData fetch(String customerId);
}
