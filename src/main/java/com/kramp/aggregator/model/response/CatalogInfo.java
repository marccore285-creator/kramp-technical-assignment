package com.kramp.aggregator.model.response;

import com.kramp.aggregator.model.upstream.CatalogData;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class CatalogInfo {

    public static final String STATUS_OK          = "OK";
    public static final String STATUS_UNAVAILABLE = "UNAVAILABLE";

    String name;
    String description;
    Map<String, String> specs;
    List<String> images;
    String status;

    public static CatalogInfo from(CatalogData data) {
        return CatalogInfo.builder()
                .name(data.name())
                .description(data.description())
                .specs(data.specs())
                .images(data.images())
                .status(STATUS_OK)
                .build();
    }
}
