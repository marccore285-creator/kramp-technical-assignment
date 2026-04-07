package com.kramp.aggregator.model.response;

import com.kramp.aggregator.model.upstream.PricingData;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PricingInfo {

    public static final String STATUS_OK          = "OK";
    public static final String STATUS_UNAVAILABLE = "UNAVAILABLE";

    Double basePrice;
    Double discount;
    Double finalPrice;
    String currency;
    String status;

    public static PricingInfo from(PricingData data, String currency) {
        return PricingInfo.builder()
                .basePrice(data.basePrice())
                .discount(data.discount())
                .finalPrice(data.finalPrice())
                .currency(currency)
                .status(STATUS_OK)
                .build();
    }

    // Currency is preserved so the UI can still render the correct symbol.
    public static PricingInfo unavailable(String currency) {
        return PricingInfo.builder()
                .currency(currency)
                .status(STATUS_UNAVAILABLE)
                .build();
    }
}
