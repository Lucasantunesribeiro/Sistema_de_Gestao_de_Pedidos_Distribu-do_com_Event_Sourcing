# Dockerfile simplificado para gestão de pedidos
# Otimizado para deploy no Render

# Stage 1: Build unified application
FROM maven:3.9.8-eclipse-temurin-17 AS java-builder
WORKDIR /app

# Copy unified-order-system source
COPY unified-order-system/ ./unified-order-system/

# Build unified application
WORKDIR /app/unified-order-system
RUN mvn clean package -DskipTests -B

# Stage 2: Build frontend
FROM node:18-alpine AS frontend-builder
WORKDIR /app

# Copy frontend files
COPY frontend/ ./frontend/

# Build frontend
WORKDIR /app/frontend
RUN npm install && npm run build

# Stage 3: Runtime environment
FROM eclipse-temurin:17-jdk-alpine
ENV DEBIAN_FRONTEND=noninteractive
WORKDIR /app

# Install runtime dependencies
RUN apk add --no-cache nginx supervisor curl gettext

# Create required directories
RUN mkdir -p /app/services /app/frontend /var/log/supervisor /etc/supervisor/conf.d

# Copy unified application JAR
COPY --from=java-builder /app/unified-order-system/target/unified-order-system-1.0.0.jar /app/unified-order-system.jar

# Copy frontend build output
COPY --from=frontend-builder /app/frontend/dist /app/frontend

# Copy nginx template and supervisor config
COPY deploy/nginx/nginx.conf.template /etc/nginx/nginx.conf.template
COPY deploy/supervisord/web.conf /etc/supervisor/conf.d/web.conf

# Set permissions
RUN chmod 644 /etc/supervisor/conf.d/*.conf && chmod 755 /app/unified-order-system.jar

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
        log "Starting unified web service (nginx + unified-order-system)"\n\
        export PORT=${PORT:-8080}\n\
        log "Processing nginx template with PORT=$PORT"\n\
        envsubst "\\$PORT" < /etc/nginx/nginx.conf.template > /tmp/nginx.conf.new\n\
        mv /tmp/nginx.conf.new /etc/nginx/nginx.conf\n\
        log "Testing nginx configuration"\n\
        nginx -t || { log "Invalid nginx.conf generated"; cat /etc/nginx/nginx.conf; exit 1; }\n\
        CONFIG="/etc/supervisor/conf.d/web.conf"\n\
        ;;\n\
    *)\n\
        log "ERROR: Invalid SERVICE_TYPE: [$SERVICE_TYPE]. Valid: web"\n\
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