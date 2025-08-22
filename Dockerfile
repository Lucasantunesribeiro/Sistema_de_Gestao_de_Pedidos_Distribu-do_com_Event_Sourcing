FROM openjdk:17-jdk-slim as builder

WORKDIR /app

# Copy all source code
COPY . .

# Build shared-events first
RUN cd shared-events && mvn clean install -DskipTests

# Build all services
RUN cd services/order-service && mvn clean package -DskipTests
RUN cd services/payment-service && mvn clean package -DskipTests  
RUN cd services/inventory-service && mvn clean package -DskipTests
RUN cd services/order-query-service && mvn clean package -DskipTests

FROM openjdk:17-jre-slim

WORKDIR /app

# Copy built jars
COPY --from=builder /app/services/order-service/target/order-service-1.0.0.jar ./
COPY --from=builder /app/services/payment-service/target/payment-service-1.0.0.jar ./
COPY --from=builder /app/services/inventory-service/target/inventory-service-1.0.0.jar ./
COPY --from=builder /app/services/order-query-service/target/order-query-service-1.0.0.jar ./

EXPOSE 8080

# Default to order-service (will be overridden by render.yaml)
CMD ["java", "-jar", "order-service-1.0.0.jar"]
