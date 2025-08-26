# Multi-stage build for Order Service
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy shared events first (for layer caching)
COPY shared-events ./shared-events
WORKDIR /app/shared-events
RUN mvn clean install -DskipTests

# Copy and build order service
WORKDIR /app
COPY services/order-service/pom.xml ./services/order-service/
COPY services/order-service/src ./services/order-service/src

WORKDIR /app/services/order-service
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
COPY --from=builder --chown=appuser:appgroup /app/services/order-service/target/*.jar app.jar

# Switch to non-root user
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

# Environment variables
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

EXPOSE 8081

CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]