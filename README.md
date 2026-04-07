# Product Information Aggregator

A production-ready backend service that combines data from four internal services
(Catalog, Pricing, Availability, Customer) into a single, market-aware response for a
B2B agricultural-parts e-commerce platform.

---

## How to run

### Prerequisites

| Tool | Version |
|------|---------|
| Java | 17+ |
| Maven | 3.9+ |
| Docker | any recent |

### Run locally with Maven

```bash
mvn spring-boot:run
```

The service starts on:
- **REST** → `http://localhost:8080`
- **gRPC** → `localhost:9090`

### Run with Docker

```bash
# Build
docker build -t product-info-aggregator .

# Run
docker run -p 8080:8080 -p 9090:9090 product-info-aggregator
```

### Run tests

```bash
mvn test
```

---

## API

### REST

```
GET /api/v1/product-info?productId={id}&market={market}[&customerId={customerId}]
```

| Parameter | Required | Example |
|-----------|----------|---------|
| `productId` | yes | `KR-12345` |
| `market` | yes | `nl-NL`, `de-DE`, `pl-PL` |
| `customerId` | no | `CUST-001` |

**Example request**

```bash
curl "http://localhost:8080/api/v1/product-info?productId=KR-12345&market=pl-PL&customerId=CUST-001"
```

**Example response (all services healthy)**

```json
{
  "productId": "KR-12345",
  "market": "pl-PL",
  "catalog": {
    "name": "Hydraulic Filter KR-62345",
    "description": "High-quality Hydraulic Filter KR-62345 for agricultural and industrial machinery.",
    "specs": {
      "weight": "145g",
      "material": "Aluminium",
      "oemReference": "OEM-62345",
      "compatibility": "Tractor, Harvester, Sprayer"
    },
    "images": [
      "https://cdn.kramp.com/products/KR-12345/main.jpg",
      "https://cdn.kramp.com/products/KR-12345/side.jpg"
    ],
    "status": "OK"
  },
  "pricing": {
    "basePrice": 122.0,
    "discount": 12.2,
    "finalPrice": 109.8,
    "currency": "PLN",
    "status": "OK"
  },
  "availability": {
    "stockLevel": 45,
    "warehouse": "PL-WH-2",
    "expectedDeliveryDays": 2,
    "status": "OK"
  },
  "customer": {
    "customerId": "CUST-001",
    "segment": "PRO",
    "preferences": ["fast-delivery", "invoice-payment"],
    "status": "PERSONALIZED"
  }
}
```

**Degraded response (Pricing service unavailable)**

```json
{
  "pricing": {
    "currency": "PLN",
    "status": "UNAVAILABLE"
  }
}
```

**Error response (Catalog unavailable → HTTP 503)**

```json
{
  "code": "CATALOG_UNAVAILABLE",
  "message": "Catalog service timed out for product: KR-12345"
}
```

### gRPC

- **Port:** `9090`
- **Service:** `kramp.aggregator.ProductInfoService`
- **RPC:** `GetProductInfo(ProductInfoRequest) → ProductInfoResponse`
- **Proto:** [`src/main/proto/product_info.proto`](src/main/proto/product_info.proto)

Example with [grpcurl](https://github.com/fullstorydev/grpcurl):

```bash
grpcurl -plaintext \
  -d '{"product_id":"KR-12345","market":"nl-NL","customer_id":"CUST-001"}' \
  localhost:9090 \
  kramp.aggregator.ProductInfoService/GetProductInfo
```

---

## Key design decisions and trade-offs

### 1. Parallel upstream calls with CompletableFuture

All four upstream calls are fired simultaneously via `CompletableFuture.supplyAsync()` on a
dedicated thread pool. The aggregation wall-clock time is ≈ max(individual latencies) rather
than their sum — with typical latencies of 50/80/100/60 ms, the aggregated response takes
~120 ms instead of ~290 ms.

`orTimeout()` on each future ensures a slow upstream service does not hold the request
indefinitely. Timeouts are configurable per-service in `application.yml`.

### 2. Required vs optional data distinction

The design treats **Catalog as a hard dependency** and the rest as optional enrichment:

| Service | Failure behaviour |
|---------|-----------------|
| Catalog | Throws `CatalogUnavailableException` → HTTP 503 |
| Pricing | `pricing.status = "UNAVAILABLE"`, rest of response intact |
| Availability | `availability.status = "UNKNOWN"`, rest of response intact |
| Customer | `customer.status = "DEFAULT"`, non-personalized response |

The `CompletableFuture.handle()` operator makes optional degradation clean — it converts
any exception (timeout or simulated failure) into a sentinel value before the caller ever
sees it, so the aggregation code stays free of try/catch noise.

### 3. Port/Adapter pattern for upstream services

Each upstream service has an interface (`CatalogServicePort`, etc.) that `AggregationService`
depends on. The mock implementations live behind those interfaces. When the platform eventually
has real Catalog/Pricing APIs, replacing mocks with HTTP clients requires zero changes to
`AggregationService` or the REST/gRPC layer.

### 4. Realistic mock simulation

The mocks:
- Use `ThreadLocalRandom` for thread-safe jitter and failure rolls (±20% latency, configurable failure rate).
- Generate **deterministic** data from the product ID hash, so repeated calls to the same product are consistent — matching real catalog/pricing behaviour.
- Failure rates are read from `AppProperties`, making them tunable via `application.yml` or environment variables without recompiling.

### 5. Shared aggregation core for REST and gRPC

`AggregationService` is called by both `ProductInfoController` (REST) and
`ProductInfoGrpcService` (gRPC). Both APIs are thin translation layers: they receive
the domain response and convert it to their wire format. Zero duplication of business logic.

---

## What I would do differently with more time

1. **Caching** — Add a short-lived cache (e.g. Caffeine, ~30 s TTL) in front of Catalog and
   Pricing calls. Catalog data changes rarely; pricing changes at most a few times per day.
   This would slash latency for hot products.

2. **Circuit breaker** — Wrap each upstream call in a Resilience4j circuit breaker. Currently,
   a service that fails 100% of the time still incurs full timeout delay on every request.
   A circuit breaker opens after N consecutive failures and returns immediately, keeping the
   aggregator responsive.

3. **Structured logging / tracing** — Add a correlation ID (`X-Request-ID` header) propagated
   through all log lines and gRPC metadata, enabling end-to-end request tracing in a log
   aggregator (e.g. Cloud Logging, Datadog).

4. **Market validation** — Validate that the `market` parameter is a known market code
   and return a clear 400 rather than silently defaulting to EUR.

5. **Integration tests** — Add Spring Boot integration tests (`@SpringBootTest`) that wire
   the full context and exercise the actual mock services (with zero failure rate) to verify
   the complete request path.

6. **gRPC reflection** — Enable gRPC server reflection (`grpc-services: REFLECTION`) so
   tools like grpcurl work without the proto file.

---

## Adding a new data source

The architecture makes this a four-step change:

1. Define the interface: `src/…/service/port/RelatedProductsServicePort.java`
2. Write the mock: `src/…/service/upstream/MockRelatedProductsService.java`
3. Add a field to `ProductInfoResponse` and a new `RelatedProductsInfo` response class.
4. In `AggregationService.aggregate()`:
   - Fire `supplyWithTimeout(() → relatedProductsService.fetch(productId, market), timeout)`.
   - Collect the result with `.handle(...)` (optional pattern).

No existing classes change.

---

## Design question — Option A: Adding a Related Products service

> *"The Assortment team wants to add a 'Related Products' service (200 ms latency, 90% reliability).  
> How would your design accommodate this? Should it be required or optional?"*

**It should be optional.** Related products are enrichment; a customer must still be able to
view and purchase the product they searched for even if the suggestion engine is down.
90% reliability means 1 in 10 requests would fail — making it required would cause a 10%
hard failure rate on every page load, which is unacceptable.

**How the current design accommodates it:**

- Fire it in parallel with the existing four calls (adds zero latency on the happy path since
  the availability service at 100 ms already determines the wall-clock time for most responses;
  200 ms would slightly increase the ceiling, but with a 300–400 ms timeout it stays within
  the SLA).
- Wrap the future in `.handle(...)` — on failure, set `relatedProducts.status = "UNAVAILABLE"`
  and return an empty list.
- Add `RelatedProductsInfo` to `ProductInfoResponse` and a new port interface.

The only production concern is that adding a 200 ms call raises the p99 response time.
Mitigations: set a tight timeout (≤250 ms), and consider prefetching / caching related
products separately if p99 matters.
