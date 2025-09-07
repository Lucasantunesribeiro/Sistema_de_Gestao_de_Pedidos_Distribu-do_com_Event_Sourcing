# ===== FRONTEND BUILD =====
FROM node:20-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# ===== BACKEND BUILD =====
FROM maven:3.9.6-eclipse-temurin-21 AS backend
WORKDIR /app/backend
COPY unified-order-system/pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline
COPY unified-order-system/ ./
RUN mvn -q -DskipTests package

# ===== RUNTIME (NGINX + JRE) =====
FROM eclipse-temurin:21-jre-alpine AS runtime
RUN apk add --no-cache nginx curl bash
WORKDIR /app

# Frontend
COPY --from=frontend /app/frontend/dist /usr/share/nginx/html

# Backend
COPY --from=backend /app/backend/target/*.jar /app/app.jar

# Nginx config and start script
COPY ops/nginx.conf /etc/nginx/nginx.conf
COPY ops/start.sh /app/start.sh
RUN chmod +x /app/start.sh

EXPOSE 80
CMD ["/app/start.sh"]

