package com.kramp.aggregator.model.response;

import com.kramp.aggregator.model.upstream.AvailabilityData;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AvailabilityInfo {

    public static final String STATUS_OK      = "OK";
    public static final String STATUS_UNKNOWN = "UNKNOWN";

    Integer stockLevel;
    String warehouse;
    Integer expectedDeliveryDays;
    String status;

    public static AvailabilityInfo from(AvailabilityData data) {
        return AvailabilityInfo.builder()
                .stockLevel(data.stockLevel())
                .warehouse(data.warehouse())
                .expectedDeliveryDays(data.expectedDeliveryDays())
                .status(STATUS_OK)
                .build();
    }

    public static AvailabilityInfo unknown() {
        return AvailabilityInfo.builder()
                .status(STATUS_UNKNOWN)
                .build();
    }
}
