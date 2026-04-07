package com.kramp.aggregator.grpc;

import com.kramp.aggregator.exception.CatalogUnavailableException;
import com.kramp.aggregator.model.response.ProductInfoResponse;
import com.kramp.aggregator.service.AggregationService;

// Proto-generated classes live in com.kramp.aggregator.proto
import com.kramp.aggregator.proto.ProductInfoServiceGrpc;
import com.kramp.aggregator.proto.ProductInfoRequest;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;

/**
 * gRPC facade over {@link AggregationService}. Handles proto mapping and
 * status code translation; aggregation logic stays in the service layer.
 */
@GrpcService
@Slf4j
@RequiredArgsConstructor
public class ProductInfoGrpcService extends ProductInfoServiceGrpc.ProductInfoServiceImplBase {

    private final AggregationService aggregationService;

    @Override
    public void getProductInfo(
            ProductInfoRequest request,
            StreamObserver<com.kramp.aggregator.proto.ProductInfoResponse> responseObserver) {

        log.debug("gRPC request: productId={}, market={}", request.getProductId(), request.getMarket());

        try {
            // Empty string in proto = absent optional field
            String customerId = request.getCustomerId().isBlank() ? null : request.getCustomerId();

            ProductInfoResponse domain = aggregationService.aggregate(
                    request.getProductId(),
                    request.getMarket(),
                    customerId);

            responseObserver.onNext(toProto(domain));
            responseObserver.onCompleted();

        } catch (CatalogUnavailableException e) {
            log.error("gRPC: catalog unavailable: {}", e.getMessage());
            responseObserver.onError(
                    Status.UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException());

        } catch (Exception e) {
            log.error("gRPC: unexpected error: {}", e.getMessage(), e);
            responseObserver.onError(
                    Status.INTERNAL.withDescription("Internal server error").asRuntimeException());
        }
    }

    private com.kramp.aggregator.proto.ProductInfoResponse toProto(ProductInfoResponse r) {
        var builder = com.kramp.aggregator.proto.ProductInfoResponse.newBuilder()
                .setProductId(r.getProductId())
                .setMarket(r.getMarket());

        if (r.getCatalog() != null) {
            var cb = com.kramp.aggregator.proto.CatalogInfo.newBuilder()
                    .setName(r.getCatalog().getName())
                    .setDescription(r.getCatalog().getDescription())
                    .setStatus(r.getCatalog().getStatus())
                    .addAllImages(nullSafe(r.getCatalog().getImages()));
            if (r.getCatalog().getSpecs() != null) {
                cb.putAllSpecs(r.getCatalog().getSpecs());
            }
            builder.setCatalog(cb.build());
        }

        if (r.getPricing() != null) {
            var pb = com.kramp.aggregator.proto.PricingInfo.newBuilder()
                    .setCurrency(nullSafeStr(r.getPricing().getCurrency()))
                    .setStatus(r.getPricing().getStatus());
            if (r.getPricing().getBasePrice() != null) {
                pb.setBasePrice(r.getPricing().getBasePrice())
                  .setDiscount(r.getPricing().getDiscount())
                  .setFinalPrice(r.getPricing().getFinalPrice());
            }
            builder.setPricing(pb.build());
        }

        if (r.getAvailability() != null) {
            var ab = com.kramp.aggregator.proto.AvailabilityInfo.newBuilder()
                    .setStatus(r.getAvailability().getStatus());
            if (r.getAvailability().getWarehouse() != null) {
                ab.setWarehouse(r.getAvailability().getWarehouse())
                  .setStockLevel(r.getAvailability().getStockLevel())
                  .setExpectedDeliveryDays(r.getAvailability().getExpectedDeliveryDays());
            }
            builder.setAvailability(ab.build());
        }

        if (r.getCustomer() != null) {
            var kb = com.kramp.aggregator.proto.CustomerInfo.newBuilder()
                    .setStatus(r.getCustomer().getStatus())
                    .addAllPreferences(nullSafe(r.getCustomer().getPreferences()));
            if (r.getCustomer().getCustomerId() != null) {
                kb.setCustomerId(r.getCustomer().getCustomerId())
                  .setSegment(nullSafeStr(r.getCustomer().getSegment()));
            }
            builder.setCustomer(kb.build());
        }

        return builder.build();
    }

    private static List<String> nullSafe(List<String> list) {
        return list != null ? list : List.of();
    }

    private static String nullSafeStr(String s) {
        return s != null ? s : "";
    }
}
