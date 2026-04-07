package com.kramp.aggregator.service.upstream;

import com.kramp.aggregator.config.AppProperties;
import com.kramp.aggregator.model.upstream.CatalogData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockCatalogServiceTest {

    private MockCatalogService service;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getFailureRates().setCatalog(0.0);
        service = new MockCatalogService(properties);
    }

    @Test
    void fetch_returnsCompleteProductData() {
        CatalogData result = service.fetch("PROD-001", "nl-NL");

        assertThat(result).isNotNull();
        assertThat(result.productId()).isEqualTo("PROD-001");
        assertThat(result.name()).isNotBlank();
        assertThat(result.description()).isNotBlank();
        assertThat(result.specs()).isNotEmpty();
        assertThat(result.images()).isNotEmpty();
        assertThat(result.images()).allMatch(url -> url.startsWith("https://"));
    }

    @Test
    void fetch_sameProductId_returnsDeterministicData() {
        CatalogData first  = service.fetch("CONSISTENT-123", "nl-NL");
        CatalogData second = service.fetch("CONSISTENT-123", "de-DE");

        assertThat(first.name()).isEqualTo(second.name());
        assertThat(first.specs()).isEqualTo(second.specs());
    }

    @Test
    void fetch_differentProductIds_returnsDifferentData() {
        CatalogData p1 = service.fetch("AAA-001", "nl-NL");
        CatalogData p2 = service.fetch("ZZZ-999", "nl-NL");

        assertThat(p1.productId()).isNotEqualTo(p2.productId());
    }

    @Test
    void fetch_specsContainExpectedKeys() {
        CatalogData result = service.fetch("SPEC-TEST", "nl-NL");

        assertThat(result.specs())
                .containsKey("weight")
                .containsKey("material")
                .containsKey("oemReference");
    }

    @RepeatedTest(3)
    void fetch_imagesAlwaysContainProductId() {
        String productId = "IMG-TEST-42";
        CatalogData result = service.fetch(productId, "nl-NL");

        result.images().forEach(url -> assertThat(url).contains(productId));
    }
}
