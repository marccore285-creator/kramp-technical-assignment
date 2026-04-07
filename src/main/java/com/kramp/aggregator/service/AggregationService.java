package com.kramp.aggregator.service;

import com.kramp.aggregator.config.AppProperties;
import com.kramp.aggregator.exception.CatalogUnavailableException;
import com.kramp.aggregator.model.response.*;
import com.kramp.aggregator.model.upstream.*;
import com.kramp.aggregator.service.port.*;
import com.kramp.aggregator.util.MarketUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Fires all upstream calls in parallel and assembles the response.
 * Catalog is required (503 on failure); Pricing, Availability, and Customer
 * are optional and fall back to degraded sentinel values.
 */
@Service
@Slf4j
public class AggregationService {

    private final CatalogServicePort      catalogService;
    private final PricingServicePort      pricingService;
    private final AvailabilityServicePort availabilityService;
    private final CustomerServicePort     customerService;
    private final AppProperties           properties;
    private final Executor                executor;

    // @Qualifier on a constructor param can't be handled by Lombok, so we wire manually.
    public AggregationService(
            CatalogServicePort catalogService,
            PricingServicePort pricingService,
            AvailabilityServicePort availabilityService,
            CustomerServicePort customerService,
            AppProperties properties,
            @Qualifier("aggregatorExecutor") Executor executor) {
        this.catalogService      = catalogService;
        this.pricingService      = pricingService;
        this.availabilityService = availabilityService;
        this.customerService     = customerService;
        this.properties          = properties;
        this.executor            = executor;
    }

    public ProductInfoResponse aggregate(String productId, String market, String customerId) {
        log.info("Aggregating: productId={}, market={}, customerId={}",
                productId, market, StringUtils.hasText(customerId) ? customerId : "anonymous");
        long start = System.currentTimeMillis();

        CompletableFuture<CatalogData> catalogFuture =
                supplyWithTimeout(() -> catalogService.fetch(productId, market),
                        properties.getTimeouts().getCatalog());

        CompletableFuture<PricingData> pricingFuture =
                supplyWithTimeout(() -> pricingService.fetch(productId, market, customerId),
                        properties.getTimeouts().getPricing());

        CompletableFuture<AvailabilityData> availabilityFuture =
                supplyWithTimeout(() -> availabilityService.fetch(productId, market),
                        properties.getTimeouts().getAvailability());

        CompletableFuture<CustomerData> customerFuture = StringUtils.hasText(customerId)
                ? supplyWithTimeout(() -> customerService.fetch(customerId),
                        properties.getTimeouts().getCustomer())
                : CompletableFuture.completedFuture(null);

        CatalogData catalogData = getCatalogOrFail(catalogFuture, productId);

        String currency = MarketUtils.getCurrency(market);

        PricingInfo      pricing      = resolvePricing(pricingFuture, currency);
        AvailabilityInfo availability = resolveAvailability(availabilityFuture);
        CustomerInfo     customer     = resolveCustomer(customerFuture, customerId);

        log.info("Aggregation complete: productId={} in {}ms (pricing={}, availability={}, customer={})",
                productId, System.currentTimeMillis() - start,
                pricing.getStatus(), availability.getStatus(), customer.getStatus());

        return ProductInfoResponse.builder()
                .productId(productId)
                .market(market)
                .catalog(CatalogInfo.from(catalogData))
                .pricing(pricing)
                .availability(availability)
                .customer(customer)
                .build();
    }

    private <T> CompletableFuture<T> supplyWithTimeout(Supplier<T> supplier, long timeoutMs) {
        return CompletableFuture
                .supplyAsync(supplier, executor)
                .orTimeout(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    private CatalogData getCatalogOrFail(CompletableFuture<CatalogData> future, String productId) {
        try {
            return future.join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof TimeoutException) {
                log.error("Catalog service TIMED OUT for productId={}", productId);
                throw new CatalogUnavailableException(
                        "Catalog service timed out for product: " + productId);
            }
            log.error("Catalog service FAILED for productId={}: {}", productId, cause.getMessage());
            throw new CatalogUnavailableException(
                    "Catalog service unavailable: " + cause.getMessage(), cause);
        }
    }

    private PricingInfo resolvePricing(CompletableFuture<PricingData> future, String currency) {
        return future.handle((data, ex) -> {
            if (ex != null) {
                log.warn("Pricing service degraded: {}", unwrap(ex).getMessage());
                return PricingInfo.unavailable(currency);
            }
            return PricingInfo.from(data, currency);
        }).join();
    }

    private AvailabilityInfo resolveAvailability(CompletableFuture<AvailabilityData> future) {
        return future.handle((data, ex) -> {
            if (ex != null) {
                log.warn("Availability service degraded: {}", unwrap(ex).getMessage());
                return AvailabilityInfo.unknown();
            }
            return AvailabilityInfo.from(data);
        }).join();
    }

    private CustomerInfo resolveCustomer(CompletableFuture<CustomerData> future, String customerId) {
        if (!StringUtils.hasText(customerId)) {
            return CustomerInfo.defaultResponse();
        }
        return future.handle((data, ex) -> {
            if (ex != null) {
                log.warn("Customer service degraded for customerId={}: {}",
                        customerId, unwrap(ex).getMessage());
                return CustomerInfo.defaultResponse();
            }
            return CustomerInfo.from(data);
        }).join();
    }

    private static Throwable unwrap(Throwable t) {
        return (t instanceof CompletionException && t.getCause() != null) ? t.getCause() : t;
    }
}
