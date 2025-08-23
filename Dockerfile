FROM maven:3.8.5-openjdk-17 as build
WORKDIR /app

# Copy all services and shared components
COPY pom.xml ./
COPY shared-events/ shared-events/
COPY services/order-service/ services/order-service/
COPY services/payment-service/ services/payment-service/
COPY services/inventory-service/ services/inventory-service/
COPY services/order-query-service/ services/order-query-service/

# Build shared events first
RUN cd shared-events && mvn clean install -DskipTests -q

# Build all services
RUN cd services/order-service && mvn clean package -DskipTests -q
RUN cd services/payment-service && mvn clean package -DskipTests -q  
RUN cd services/inventory-service && mvn clean package -DskipTests -q
RUN cd services/order-query-service && mvn clean package -DskipTests -q

# Production stage
FROM openjdk:17-jdk-alpine
WORKDIR /app

# Install process manager and utilities
RUN apk add --no-cache supervisor netcat-openbsd

# Copy all service JARs
COPY --from=build /app/services/order-service/target/order-service-1.0.0.jar order-service.jar
COPY --from=build /app/services/payment-service/target/payment-service-1.0.0.jar payment-service.jar
COPY --from=build /app/services/inventory-service/target/inventory-service-1.0.0.jar inventory-service.jar
COPY --from=build /app/services/order-query-service/target/order-query-service-1.0.0.jar query-service.jar

# Create supervisor configuration
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf

# Create startup script
COPY start-all-services.sh /app/start-all-services.sh
RUN chmod +x /app/start-all-services.sh

# Expose all ports
EXPOSE 8080 8081 8082 8083 8084

# Start all services
CMD ["/app/start-all-services.sh"]