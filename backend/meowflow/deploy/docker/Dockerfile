# Multi-module Dockerfile for MeowFlow microservices
# Build all modules at once, then select the target service jar via SERVICE_NAME arg.
#
# Build examples:
#   docker build --build-arg SERVICE_NAME=meowflow-gateway -f deploy/docker/Dockerfile -t meowflow-gateway .
#   docker build --build-arg SERVICE_NAME=meowflow-user    -f deploy/docker/Dockerfile -t meowflow-user .
#   docker build --build-arg SERVICE_NAME=meowflow-workflow -f deploy/docker/Dockerfile -t meowflow-workflow .

# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS builder

ARG SERVICE_NAME=meowflow-gateway

WORKDIR /build

# Copy root pom first for dependency caching
COPY pom.xml ./
COPY meowflow-common/pom.xml        ./meowflow-common/
COPY meowflow-gateway/pom.xml       ./meowflow-gateway/
COPY meowflow-workflow/pom.xml      ./meowflow-workflow/
COPY meowflow-executor/pom.xml      ./meowflow-executor/
COPY meowflow-user/pom.xml          ./meowflow-user/
COPY meowflow-template/pom.xml      ./meowflow-template/
COPY meowflow-monitor/pom.xml       ./meowflow-monitor/
COPY meowflow-infra/pom.xml         ./meowflow-infra/

# Download dependencies (cached)
RUN mvn dependency:go-offline -B -Dmaven.test.skip=true || true

# Copy full source and build only the target service
COPY . .

RUN mvn package -pl ${SERVICE_NAME} -am -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre AS runtime

ARG SERVICE_NAME=meowflow-gateway

WORKDIR /app

RUN groupadd -r meowflow && useradd -r -g meowflow meowflow \
    && apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /build/${SERVICE_NAME}/target/${SERVICE_NAME}-exec.jar app.jar

# Copy scripts/sql for PostgreSQL initialization
COPY scripts/sql/ /docker-entrypoint-initdb.d/scripts/sql/

RUN chown -R meowflow:meowflow /app
USER meowflow

ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC"

# Default to gateway port; each service Dockerfile overrides via ENV
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
