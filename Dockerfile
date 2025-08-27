# Multi-stage Dockerfile for unified frontend + backend deployment
# Stage 1: Build shared events
FROM maven:3.9.8-eclipse-temurin-17 AS shared-builder
WORKDIR /app
COPY pom.xml ./
COPY shared-events/ shared-events/
RUN cd shared-events && mvn clean install -DskipTests -B

# Stage 2: Build all Java services  
FROM maven:3.9.8-eclipse-temurin-17 AS java-builder
WORKDIR /app

# Copy Maven repository from shared-builder
COPY --from=shared-builder /root/.m2/repository /root/.m2/repository

# Copy Maven configuration files for cache optimization
COPY pom.xml ./
COPY shared-events/ shared-events/
COPY services/order-service/pom.xml services/order-service/
COPY services/payment-service/pom.xml services/payment-service/
COPY services/inventory-service/pom.xml services/inventory-service/
COPY services/order-query-service/pom.xml services/order-query-service/

# Download dependencies for cache layer
RUN mvn -B -f pom.xml -DskipTests dependency:resolve

# Copy source code
COPY services/ services/

# Build all services
RUN mvn -B -f pom.xml clean package -DskipTests

# Stage 3: Build React frontend
FROM node:18-alpine AS frontend-builder
WORKDIR /app/frontend
COPY frontend/package.json ./
RUN npm install
COPY frontend/ .
RUN npm run build

# Stage 4: Runtime environment with Nginx + Java services
FROM eclipse-temurin:17-jdk-alpine
RUN apk add --no-cache nginx supervisor curl gettext

# Create directories
RUN mkdir -p /app/services /app/frontend /var/log/supervisor /etc/supervisor/conf.d

# Copy built JAR files with exact names
COPY --from=java-builder /app/services/order-service/target/order-service-1.0.0.jar /app/services/order-service.jar
COPY --from=java-builder /app/services/payment-service/target/payment-service-1.0.0.jar /app/services/payment-service.jar  
COPY --from=java-builder /app/services/inventory-service/target/inventory-service-1.0.0.jar /app/services/inventory-service.jar
COPY --from=java-builder /app/services/order-query-service/target/order-query-service-1.0.0.jar /app/services/query-service.jar

# Copy built frontend
COPY --from=frontend-builder /app/frontend/dist /app/frontend

# Copy nginx template and all supervisor configs
COPY deploy/nginx/nginx.conf.template /etc/nginx/nginx.conf.template
COPY deploy/supervisord/ /etc/supervisor/configs/
RUN chmod 644 /etc/supervisor/configs/*.conf

# Create startup script with service type selection
RUN printf '#!/bin/sh\n\
export PORT=${PORT:-80}\n\
export SERVICE_TYPE=${SERVICE_TYPE:-web}\n\
\n\
echo "=== Configurando deploy para SERVICE_TYPE=$SERVICE_TYPE ==="\n\
\n\
# Select appropriate supervisord config based on SERVICE_TYPE\n\
case "$SERVICE_TYPE" in\n\
  web)\n\
    SUPERVISOR_CONF="/etc/supervisor/configs/web.conf"\n\
    echo "Web service: nginx + query-service na porta $PORT"\n\
    # Process nginx template only for web service\n\
    envsubst < /etc/nginx/nginx.conf.template > /etc/nginx/nginx.conf\n\
    ;;\n\
  order)\n\
    SUPERVISOR_CONF="/etc/supervisor/configs/order.conf"\n\
    echo "Order service isolado na porta 8081"\n\
    ;;\n\
  payment)\n\
    SUPERVISOR_CONF="/etc/supervisor/configs/payment.conf"\n\
    echo "Payment service isolado na porta 8082"\n\
    ;;\n\
  inventory)\n\
    SUPERVISOR_CONF="/etc/supervisor/configs/inventory.conf"\n\
    echo "Inventory service isolado na porta 8083"\n\
    ;;\n\
  *)\n\
    echo "ERROR: Invalid SERVICE_TYPE=$SERVICE_TYPE. Valid values: web, order, payment, inventory"\n\
    exit 1\n\
    ;;\n\
esac\n\
\n\
# Validate selected config contains required [supervisord] section\n\
if ! grep -q "^\[supervisord\]" "$SUPERVISOR_CONF"; then\n\
  echo "ERROR: $SUPERVISOR_CONF missing [supervisord] section"\n\
  cat "$SUPERVISOR_CONF"\n\
  exit 1\n\
fi\n\
\n\
echo "Iniciando supervisord com config: $SUPERVISOR_CONF"\n\
exec /usr/bin/supervisord -c "$SUPERVISOR_CONF"\n\
' > /start.sh && chmod +x /start.sh

# Expose ports based on service type (handled by runtime ENV)
EXPOSE 80 8081 8082 8083 8084

# Health check varies by service type (handled by startup script)  
CMD ["/start.sh"]