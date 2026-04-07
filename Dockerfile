# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Cache the dependency layer separately so rebuilds don't re-download the world
COPY pom.xml .
COPY .mvn ./.mvn
COPY src ./src

RUN mvn clean package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /app/target/product-info-aggregator-1.0.0-SNAPSHOT.jar app.jar

# 8080 = REST (HTTP)   9090 = gRPC
EXPOSE 8080 9090

ENTRYPOINT ["java", "-jar", "app.jar"]
