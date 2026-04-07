package com.kramp.aggregator.model.upstream;

import java.util.List;

/** Raw data returned by the upstream Customer service. */
public record CustomerData(
        String customerId,
        String segment,
        List<String> preferences
) {}
