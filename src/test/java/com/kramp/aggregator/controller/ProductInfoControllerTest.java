package com.kramp.aggregator.controller;

import com.kramp.aggregator.exception.CatalogUnavailableException;
import com.kramp.aggregator.exception.GlobalExceptionHandler;
import com.kramp.aggregator.model.response.*;
import com.kramp.aggregator.service.AggregationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductInfoController.class)
@Import(GlobalExceptionHandler.class)
class ProductInfoControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean  AggregationService aggregationService;

    @Test
    void validRequest_returns200WithAggregatedData() throws Exception {
        when(aggregationService.aggregate("P1", "nl-NL", null)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/product-info")
                        .param("productId", "P1")
                        .param("market", "nl-NL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("P1"))
                .andExpect(jsonPath("$.market").value("nl-NL"))
                .andExpect(jsonPath("$.catalog.status").value("OK"))
                .andExpect(jsonPath("$.catalog.name").value("Test Product"))
                .andExpect(jsonPath("$.pricing.status").value("OK"))
                .andExpect(jsonPath("$.pricing.currency").value("EUR"))
                .andExpect(jsonPath("$.availability.status").value("OK"))
                .andExpect(jsonPath("$.customer.status").value("DEFAULT"));
    }

    @Test
    void withCustomerId_passesThroughToAggregationService() throws Exception {
        when(aggregationService.aggregate("P1", "nl-NL", "C1")).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/product-info")
                        .param("productId", "P1")
                        .param("market", "nl-NL")
                        .param("customerId", "C1"))
                .andExpect(status().isOk());

        verify(aggregationService).aggregate("P1", "nl-NL", "C1");
    }

    @Test
    void missingProductId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/product-info")
                        .param("market", "nl-NL"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingMarket_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/product-info")
                        .param("productId", "P1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void catalogUnavailable_returns503WithErrorBody() throws Exception {
        when(aggregationService.aggregate(any(), any(), any()))
                .thenThrow(new CatalogUnavailableException("Catalog service timed out"));

        mockMvc.perform(get("/api/v1/product-info")
                        .param("productId", "P1")
                        .param("market", "nl-NL"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("CATALOG_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void degradedPricing_stillReturns200() throws Exception {
        ProductInfoResponse degraded = ProductInfoResponse.builder()
                .productId("P1")
                .market("nl-NL")
                .catalog(sampleCatalog())
                .pricing(PricingInfo.unavailable("EUR"))
                .availability(AvailabilityInfo.from(new com.kramp.aggregator.model.upstream.AvailabilityData(5, "NL-WH-1", 2)))
                .customer(CustomerInfo.defaultResponse())
                .build();

        when(aggregationService.aggregate("P1", "nl-NL", null)).thenReturn(degraded);

        mockMvc.perform(get("/api/v1/product-info")
                        .param("productId", "P1")
                        .param("market", "nl-NL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pricing.status").value("UNAVAILABLE"));
    }

    private ProductInfoResponse sampleResponse() {
        return ProductInfoResponse.builder()
                .productId("P1")
                .market("nl-NL")
                .catalog(sampleCatalog())
                .pricing(PricingInfo.builder()
                        .basePrice(100.0).discount(0.0).finalPrice(100.0)
                        .currency("EUR").status(PricingInfo.STATUS_OK).build())
                .availability(AvailabilityInfo.builder()
                        .stockLevel(10).warehouse("NL-WH-1").expectedDeliveryDays(2)
                        .status(AvailabilityInfo.STATUS_OK).build())
                .customer(CustomerInfo.defaultResponse())
                .build();
    }

    private CatalogInfo sampleCatalog() {
        return CatalogInfo.builder()
                .name("Test Product")
                .description("A test product")
                .specs(Map.of("weight", "100g"))
                .images(List.of("https://cdn.kramp.com/products/P1/main.jpg"))
                .status(CatalogInfo.STATUS_OK)
                .build();
    }
}
