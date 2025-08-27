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

# Copy nginx template and supervisor config
COPY deploy/nginx/nginx.conf.template /etc/nginx/nginx.conf.template
COPY deploy/supervisord/supervisord.conf /etc/supervisor/supervisord.conf
RUN chmod 644 /etc/supervisor/supervisord.conf

# Validate supervisord.conf contains required [supervisord] section
RUN grep -q '^\[supervisord\]' /etc/supervisor/supervisord.conf || (echo "ERROR: supervisord.conf missing [supervisord]" && cat /etc/supervisor/supervisord.conf && false)

# Create startup script to process nginx template
RUN echo '#!/bin/sh\nexport PORT=${PORT:-80}\nenvsubst < /etc/nginx/nginx.conf.template > /etc/nginx/nginx.conf\nexec /usr/bin/supervisord -c /etc/supervisor/supervisord.conf' > /start.sh
RUN chmod +x /start.sh

EXPOSE ${PORT:-80}
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s CMD curl -f http://localhost:${PORT:-80}/health || exit 1
CMD ["/start.sh"]