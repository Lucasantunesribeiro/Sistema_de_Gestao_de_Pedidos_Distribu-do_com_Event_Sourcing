# Multi-stage build for Payment Service
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy shared events first (for layer caching)
COPY shared-events ./shared-events
WORKDIR /app/shared-events
RUN mvn clean install -DskipTests

# Copy and build payment service
WORKDIR /app
COPY services/payment-service/pom.xml ./services/payment-service/
COPY services/payment-service/src ./services/payment-service/src

WORKDIR /app/services/payment-service
RUN mvn clean package -DskipTests -Dspring-boot.repackage.skip=false

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

# Copy jar from builder stage
COPY --from=builder --chown=appuser:appgroup /app/services/payment-service/target/*.jar app.jar

# Switch to non-root user
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8082/actuator/health || exit 1

# Environment variables
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

EXPOSE 8082

CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]