# Multi-stage build para Sistema de Gestao de Pedidos
FROM maven:3.9.4-openjdk-17 as builder

WORKDIR /app

# Copy all source code
COPY . .

# Build shared-events first
RUN cd shared-events && mvn clean install -DskipTests -q

# Build order-service (foco em um service primeiro)
RUN cd services/order-service && mvn clean package -DskipTests -q

# Runtime stage
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy built jar
COPY --from=builder /app/services/order-service/target/order-service-1.0.0.jar ./app.jar

# Create non-root user
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

CMD ["java", "-Xmx512m", "-jar", "app.jar"]
