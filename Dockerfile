# Dockerfile simplificado para gestão de pedidos
# Otimizado para deploy no Render

# Stage 1: Build Java services
FROM maven:3.9.8-eclipse-temurin-17 AS java-builder
WORKDIR /app

# Copy all source files
COPY . .

# Build everything in one step
RUN mvn clean package -DskipTests -B

# Stage 2: Build frontend (fallback strategy)
FROM node:18-alpine AS frontend-builder
WORKDIR /app/frontend

# Copy frontend files
COPY frontend/ ./

# Try simple build first, fallback to full build if needed
RUN node build-simple.js || (npm install && npm run build)

# Stage 3: Runtime environment
FROM eclipse-temurin:17-jdk-alpine
ENV DEBIAN_FRONTEND=noninteractive
WORKDIR /app

# Install runtime dependencies
RUN apk add --no-cache nginx supervisor curl gettext

# Create required directories
RUN mkdir -p /app/services /app/frontend /var/log/supervisor /etc/supervisor/conf.d

# Copy built JAR files with exact names
COPY --from=java-builder /app/services/order-service/target/order-service-1.0.0.jar /app/services/order-service.jar
COPY --from=java-builder /app/services/payment-service/target/payment-service-1.0.0.jar /app/services/payment-service.jar
COPY --from=java-builder /app/services/inventory-service/target/inventory-service-1.0.0.jar /app/services/inventory-service.jar
COPY --from=java-builder /app/services/order-query-service/target/order-query-service-1.0.0.jar /app/services/query-service.jar

# Copy frontend build output
COPY --from=frontend-builder /app/frontend/dist /app/frontend

# Copy nginx template and supervisor configs
COPY deploy/nginx/nginx.conf.template /etc/nginx/nginx.conf.template
COPY deploy/supervisord/web.conf /etc/supervisor/conf.d/web.conf
COPY deploy/supervisord/order.conf /etc/supervisor/conf.d/order.conf
COPY deploy/supervisord/payment.conf /etc/supervisor/conf.d/payment.conf
COPY deploy/supervisord/inventory.conf /etc/supervisor/conf.d/inventory.conf

# Set permissions
RUN chmod 644 /etc/supervisor/conf.d/*.conf && chmod 755 /app/services/*.jar

# Create startup script
RUN printf '#!/bin/sh\n\
set -e\n\
log() { echo "[$(date)] SERVICE_TYPE=$SERVICE_TYPE: $1"; }\n\
\n\
# Debug environment\n\
log "=== STARTUP DEBUG ==="\n\
log "SERVICE_TYPE: ${SERVICE_TYPE:-NOT_SET}"\n\
log "PORT: ${PORT:-NOT_SET}"\n\
log "RENDER_SERVICE_TYPE: ${RENDER_SERVICE_TYPE:-NOT_SET}"\n\
log "All env vars:"\n\
env | grep -E "(SERVICE|PORT|RENDER)" | sort || echo "No matching env vars"\n\
log "=================="\n\
\n\
# Default SERVICE_TYPE if not set (fallback to web)\n\
if [ -z "$SERVICE_TYPE" ]; then\n\
    if [ ! -z "$RENDER_SERVICE_TYPE" ]; then\n\
        SERVICE_TYPE="$RENDER_SERVICE_TYPE"\n\
        log "Using RENDER_SERVICE_TYPE: $SERVICE_TYPE"\n\
    else\n\
        SERVICE_TYPE="web"\n\
        log "Defaulting to SERVICE_TYPE: web"\n\
    fi\n\
fi\n\
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
        CONFIG="/etc/supervisor/conf.d/web.conf"\n\
        ;;\n\
    "order")\n\
        log "Starting order service"\n\
        CONFIG="/etc/supervisor/conf.d/order.conf"\n\
        ;;\n\
    "payment")\n\
        log "Starting payment service"\n\
        CONFIG="/etc/supervisor/conf.d/payment.conf"\n\
        ;;\n\
    "inventory")\n\
        log "Starting inventory service"\n\
        CONFIG="/etc/supervisor/conf.d/inventory.conf"\n\
        ;;\n\
    *)\n\
        log "ERROR: Invalid SERVICE_TYPE: [$SERVICE_TYPE]. Valid: web, order, payment, inventory"\n\
        log "Available config files:"\n\
        ls -la /etc/supervisor/conf.d/\n\
        exit 1\n\
        ;;\n\
esac\n\
\n\
log "Starting supervisord with config: $CONFIG"\n\
exec /usr/bin/supervisord -c "$CONFIG"\n\
' > /app/startup.sh && chmod +x /app/startup.sh

# Expose port (will be set dynamically by Render)
EXPOSE ${PORT:-8080}

# Health checks devem ser configurados individualmente na UI do Render para cada tipo de serviço
# (web vs. workers), pois cada serviço tem diferentes portas e endpoints de saúde.
# O Render gerencia health checks automaticamente através da configuração de serviço.

# Start with our intelligent startup script
CMD ["/app/startup.sh"]