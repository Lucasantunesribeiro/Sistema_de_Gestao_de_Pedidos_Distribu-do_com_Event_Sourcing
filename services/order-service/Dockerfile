# Enhanced Multi-stage Dockerfile for Order Service
# Optimized build process with proper layer caching and runtime optimization

# ============================================================================
# BUILD STAGE - Maven build with shared-events optimization
# ============================================================================
FROM maven:3.9-openjdk-17-slim AS builder

# Set working directory
WORKDIR /app

# Copy parent pom.xml for dependency resolution
COPY pom.xml .

# Copy and build shared-events first (this layer will be cached)
COPY shared-events/ ./shared-events/
RUN cd shared-events && mvn clean install -DskipTests

# Copy service-specific pom.xml for dependency caching
COPY services/order-service/pom.xml ./services/order-service/
RUN cd services/order-service && mvn dependency:go-offline -B

# Copy service source code
COPY services/order-service/src ./services/order-service/src

# Build the service
RUN cd services/order-service && mvn clean package -DskipTests -B

# ============================================================================
# RUNTIME STAGE - Optimized JRE with health check support
# ============================================================================
FROM openjdk:17-jdk-slim AS runtime

# Install curl for health checks and clean up in same layer
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/* && \
    apt-get clean

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Set working directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/services/order-service/target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appuser /app
USER appuser

# Expose service port
EXPOSE 8081

# Configure JVM for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:+UseStringDeduplication \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=docker"

# Health check configuration
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/api/orders/health || exit 1

# Start the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]