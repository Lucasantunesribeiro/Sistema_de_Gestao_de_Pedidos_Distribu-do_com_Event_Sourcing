# Multi-stage Dockerfile para sistema distribuído de gestão de pedidos
# Otimizado para deploy no Render com SERVICE_TYPE variável

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

# Copy Maven files for dependency caching
COPY pom.xml ./
COPY shared-events/ shared-events/
COPY services/order-service/pom.xml services/order-service/
COPY services/payment-service/pom.xml services/payment-service/
COPY services/inventory-service/pom.xml services/inventory-service/
COPY services/order-query-service/pom.xml services/order-query-service/

# Download dependencies
RUN mvn -B -f pom.xml -DskipTests dependency:resolve

# Copy source code and build
COPY services/ services/
RUN mvn -B -f pom.xml clean package -DskipTests

# Stage 3: Runtime environment
FROM eclipse-temurin:17-jdk-alpine
ENV DEBIAN_FRONTEND=noninteractive
WORKDIR /app

# Install runtime dependencies
RUN apk add --no-cache nginx supervisor curl gettext

# Create required directories
RUN mkdir -p /app/services /app/frontend /var/log/supervisor /etc/supervisor

# Copy built JAR files with exact names
COPY --from=java-builder /app/services/order-service/target/order-service-1.0.0.jar /app/services/order-service.jar
COPY --from=java-builder /app/services/payment-service/target/payment-service-1.0.0.jar /app/services/payment-service.jar
COPY --from=java-builder /app/services/inventory-service/target/inventory-service-1.0.0.jar /app/services/inventory-service.jar
COPY --from=java-builder /app/services/order-query-service/target/order-query-service-1.0.0.jar /app/services/query-service.jar

# Copy frontend static files (pre-built)
COPY frontend/dist /app/frontend

# Copy nginx template and supervisor configs
COPY deploy/nginx/nginx.conf.template /etc/nginx/nginx.conf.template
COPY deploy/supervisord/web.conf /etc/supervisor/web.conf
COPY deploy/supervisord/order.conf /etc/supervisor/order.conf
COPY deploy/supervisord/payment.conf /etc/supervisor/payment.conf
COPY deploy/supervisord/inventory.conf /etc/supervisor/inventory.conf

# Set permissions
RUN chmod 644 /etc/supervisor/*.conf && chmod 755 /app/services/*.jar

# Create startup script
RUN printf '#!/bin/sh\n\
set -e\n\
log() { echo "[$(date)] SERVICE_TYPE=$SERVICE_TYPE: $1"; }\n\
\n\
case "$SERVICE_TYPE" in\n\
    "web")\n\
        log "Starting web service (nginx + query-service)"\n\
        export PORT=${PORT:-8080}\n\
        log "Processing nginx template with PORT=$PORT"\n\
        envsubst "\\$PORT" < /etc/nginx/nginx.conf.template > /tmp/nginx.conf.new\n\
        mv /tmp/nginx.conf.new /etc/nginx/nginx.conf\n\
        log "Testing nginx configuration"\n\
        nginx -t || { log "Invalid nginx.conf generated"; cat /etc/nginx/nginx.conf; exit 1; }\n\
        CONFIG="/etc/supervisor/web.conf"\n\
        ;;\n\
    "order")\n\
        log "Starting order service"\n\
        CONFIG="/etc/supervisor/order.conf"\n\
        ;;\n\
    "payment")\n\
        log "Starting payment service"\n\
        CONFIG="/etc/supervisor/payment.conf"\n\
        ;;\n\
    "inventory")\n\
        log "Starting inventory service"\n\
        CONFIG="/etc/supervisor/inventory.conf"\n\
        ;;\n\
    *)\n\
        log "ERROR: Invalid SERVICE_TYPE. Valid: web, order, payment, inventory"\n\
        exit 1\n\
        ;;\n\
esac\n\
\n\
log "Starting supervisord with config: $CONFIG"\n\
exec /usr/bin/supervisord -c "$CONFIG"\n\
' > /app/startup.sh && chmod +x /app/startup.sh

# Expose port (will be set dynamically by Render)
EXPOSE ${PORT:-8080}

# Healthcheck
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s \
  CMD curl -f http://localhost:${PORT:-8080}/health || exit 1

# Start with our intelligent startup script
CMD ["/app/startup.sh"]