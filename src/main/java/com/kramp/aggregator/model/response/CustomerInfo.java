package com.kramp.aggregator.model.response;

import com.kramp.aggregator.model.upstream.CustomerData;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class CustomerInfo {

    public static final String STATUS_PERSONALIZED = "PERSONALIZED";
    public static final String STATUS_DEFAULT      = "DEFAULT";

    String customerId;
    String segment;
    List<String> preferences;
    String status;

    public static CustomerInfo from(CustomerData data) {
        return CustomerInfo.builder()
                .customerId(data.customerId())
                .segment(data.segment())
                .preferences(data.preferences())
                .status(STATUS_PERSONALIZED)
                .build();
    }

    public static CustomerInfo defaultResponse() {
        return CustomerInfo.builder()
                .preferences(List.of())
                .status(STATUS_DEFAULT)
                .build();
    }
}
