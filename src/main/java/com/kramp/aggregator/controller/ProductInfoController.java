package com.kramp.aggregator.controller;

import com.kramp.aggregator.model.response.ProductInfoResponse;
import com.kramp.aggregator.service.AggregationService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST entry point. Validation and routing only — business logic is in
 * {@link AggregationService}, error mapping in {@link com.kramp.aggregator.exception.GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/v1")
@Validated
@Slf4j
@RequiredArgsConstructor
public class ProductInfoController {

    private final AggregationService aggregationService;

    @GetMapping("/product-info")
    public ResponseEntity<ProductInfoResponse> getProductInfo(
            @RequestParam @NotBlank(message = "productId must not be blank") String productId,
            @RequestParam @NotBlank(message = "market must not be blank") String market,
            @RequestParam(required = false) String customerId) {

        log.debug("REST request: productId={}, market={}", productId, market);
        ProductInfoResponse response = aggregationService.aggregate(productId, market, customerId);
        return ResponseEntity.ok(response);
    }
}
