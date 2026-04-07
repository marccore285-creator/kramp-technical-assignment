package com.kramp.aggregator.service;

import com.kramp.aggregator.config.AppProperties;
import com.kramp.aggregator.exception.CatalogUnavailableException;
import com.kramp.aggregator.exception.UpstreamServiceException;
import com.kramp.aggregator.model.response.*;
import com.kramp.aggregator.model.upstream.*;
import com.kramp.aggregator.service.port.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AggregationServiceTest {

    @Mock CatalogServicePort      catalogService;
    @Mock PricingServicePort      pricingService;
    @Mock AvailabilityServicePort availabilityService;
    @Mock CustomerServicePort     customerService;

    private AggregationService aggregationService;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        Executor syncExecutor = Runnable::run;
        aggregationService = new AggregationService(
                catalogService, pricingService, availabilityService,
                customerService, properties, syncExecutor);
    }

    @Test
    void allServicesSucceed_returnsFullResponse() {
        when(catalogService.fetch("P1", "nl-NL")).thenReturn(sampleCatalog());
        when(pricingService.fetch("P1", "nl-NL", null)).thenReturn(new PricingData(100.0, 0.0, 100.0));
        when(availabilityService.fetch("P1", "nl-NL")).thenReturn(new AvailabilityData(10, "NL-WH-1", 2));

        ProductInfoResponse response = aggregationService.aggregate("P1", "nl-NL", null);

        assertThat(response.getProductId()).isEqualTo("P1");
        assertThat(response.getMarket()).isEqualTo("nl-NL");
        assertThat(response.getCatalog().getStatus()).isEqualTo(CatalogInfo.STATUS_OK);
        assertThat(response.getCatalog().getName()).isEqualTo("Test Product");
        assertThat(response.getPricing().getStatus()).isEqualTo(PricingInfo.STATUS_OK);
        assertThat(response.getPricing().getCurrency()).isEqualTo("EUR");
        assertThat(response.getAvailability().getStatus()).isEqualTo(AvailabilityInfo.STATUS_OK);
        assertThat(response.getCustomer().getStatus()).isEqualTo(CustomerInfo.STATUS_DEFAULT);
        verify(customerService, never()).fetch(any());
    }

    @Test
    void withCustomerId_returnsPersonalizedResponse() {
        when(catalogService.fetch("P1", "nl-NL")).thenReturn(sampleCatalog());
        when(pricingService.fetch(eq("P1"), eq("nl-NL"), eq("C1"))).thenReturn(new PricingData(100.0, 10.0, 90.0));
        when(availabilityService.fetch("P1", "nl-NL")).thenReturn(new AvailabilityData(5, "NL-WH-2", 1));
        when(customerService.fetch("C1")).thenReturn(new CustomerData("C1", "PRO", List.of("fast-delivery")));

        ProductInfoResponse response = aggregationService.aggregate("P1", "nl-NL", "C1");

        assertThat(response.getCustomer().getStatus()).isEqualTo(CustomerInfo.STATUS_PERSONALIZED);
        assertThat(response.getCustomer().getSegment()).isEqualTo("PRO");
        assertThat(response.getCustomer().getPreferences()).containsExactly("fast-delivery");
        assertThat(response.getPricing().getDiscount()).isEqualTo(10.0);
        assertThat(response.getPricing().getFinalPrice()).isEqualTo(90.0);
    }

    @Test
    void plMarket_pricingCurrencyIsPLN() {
        when(catalogService.fetch("P1", "pl-PL")).thenReturn(sampleCatalog());
        when(pricingService.fetch(any(), any(), any())).thenReturn(new PricingData(100.0, 0.0, 100.0));
        when(availabilityService.fetch(any(), any())).thenReturn(new AvailabilityData(5, "PL-WH-1", 2));

        ProductInfoResponse response = aggregationService.aggregate("P1", "pl-PL", null);

        assertThat(response.getPricing().getCurrency()).isEqualTo("PLN");
    }

    @Test
    void catalogFails_throwsCatalogUnavailableException() {
        when(catalogService.fetch(any(), any()))
                .thenThrow(new UpstreamServiceException("Catalog down"));
        when(pricingService.fetch(any(), any(), any())).thenReturn(new PricingData(100.0, 0.0, 100.0));
        when(availabilityService.fetch(any(), any())).thenReturn(new AvailabilityData(5, "NL-WH-1", 2));

        assertThatThrownBy(() -> aggregationService.aggregate("P1", "nl-NL", null))
                .isInstanceOf(CatalogUnavailableException.class)
                .hasMessageContaining("Catalog service unavailable");
    }

    @Test
    void pricingFails_returnsDegradedResponseWithUnavailablePrice() {
        when(catalogService.fetch("P1", "nl-NL")).thenReturn(sampleCatalog());
        when(pricingService.fetch(any(), any(), any()))
                .thenThrow(new UpstreamServiceException("Pricing down"));
        when(availabilityService.fetch("P1", "nl-NL")).thenReturn(new AvailabilityData(5, "NL-WH-1", 2));

        ProductInfoResponse response = aggregationService.aggregate("P1", "nl-NL", null);

        assertThat(response.getCatalog().getStatus()).isEqualTo(CatalogInfo.STATUS_OK);
        assertThat(response.getPricing().getStatus()).isEqualTo(PricingInfo.STATUS_UNAVAILABLE);
        assertThat(response.getPricing().getBasePrice()).isNull();
        assertThat(response.getPricing().getCurrency()).isEqualTo("EUR");
        assertThat(response.getAvailability().getStatus()).isEqualTo(AvailabilityInfo.STATUS_OK);
    }

    @Test
    void availabilityFails_returnsDegradedResponseWithUnknownStock() {
        when(catalogService.fetch("P1", "nl-NL")).thenReturn(sampleCatalog());
        when(pricingService.fetch(any(), any(), any())).thenReturn(new PricingData(100.0, 0.0, 100.0));
        when(availabilityService.fetch(any(), any()))
                .thenThrow(new UpstreamServiceException("Availability down"));

        ProductInfoResponse response = aggregationService.aggregate("P1", "nl-NL", null);

        assertThat(response.getAvailability().getStatus()).isEqualTo(AvailabilityInfo.STATUS_UNKNOWN);
        assertThat(response.getAvailability().getWarehouse()).isNull();
        assertThat(response.getCatalog().getStatus()).isEqualTo(CatalogInfo.STATUS_OK);
        assertThat(response.getPricing().getStatus()).isEqualTo(PricingInfo.STATUS_OK);
    }

    @Test
    void customerServiceFails_returnsDefaultCustomerInfo() {
        when(catalogService.fetch("P1", "nl-NL")).thenReturn(sampleCatalog());
        when(pricingService.fetch(any(), any(), any())).thenReturn(new PricingData(100.0, 0.0, 100.0));
        when(availabilityService.fetch("P1", "nl-NL")).thenReturn(new AvailabilityData(5, "NL-WH-1", 2));
        when(customerService.fetch("C1")).thenThrow(new UpstreamServiceException("Customer service down"));

        ProductInfoResponse response = aggregationService.aggregate("P1", "nl-NL", "C1");

        assertThat(response.getCustomer().getStatus()).isEqualTo(CustomerInfo.STATUS_DEFAULT);
        assertThat(response.getCustomer().getCustomerId()).isNull();
    }

    @Test
    void noCustomerId_doesNotCallCustomerService_returnsDefaultCustomer() {
        when(catalogService.fetch("P1", "nl-NL")).thenReturn(sampleCatalog());
        when(pricingService.fetch(any(), any(), any())).thenReturn(new PricingData(100.0, 0.0, 100.0));
        when(availabilityService.fetch("P1", "nl-NL")).thenReturn(new AvailabilityData(5, "NL-WH-1", 2));

        ProductInfoResponse response = aggregationService.aggregate("P1", "nl-NL", null);

        assertThat(response.getCustomer().getStatus()).isEqualTo(CustomerInfo.STATUS_DEFAULT);
        verify(customerService, never()).fetch(any());
    }

    @Test
    void allOptionalServicesFail_stillReturnsValidResponse() {
        when(catalogService.fetch("P1", "nl-NL")).thenReturn(sampleCatalog());
        when(pricingService.fetch(any(), any(), any()))
                .thenThrow(new UpstreamServiceException("Pricing down"));
        when(availabilityService.fetch(any(), any()))
                .thenThrow(new UpstreamServiceException("Availability down"));
        when(customerService.fetch("C1"))
                .thenThrow(new UpstreamServiceException("Customer down"));

        ProductInfoResponse response = aggregationService.aggregate("P1", "nl-NL", "C1");

        assertThat(response.getCatalog().getStatus()).isEqualTo(CatalogInfo.STATUS_OK);
        assertThat(response.getPricing().getStatus()).isEqualTo(PricingInfo.STATUS_UNAVAILABLE);
        assertThat(response.getAvailability().getStatus()).isEqualTo(AvailabilityInfo.STATUS_UNKNOWN);
        assertThat(response.getCustomer().getStatus()).isEqualTo(CustomerInfo.STATUS_DEFAULT);
    }

    private CatalogData sampleCatalog() {
        return new CatalogData("P1", "Test Product", "A test product description",
                Map.of("weight", "100g", "material", "Steel"),
                List.of("https://cdn.kramp.com/products/P1/main.jpg"));
    }
}
