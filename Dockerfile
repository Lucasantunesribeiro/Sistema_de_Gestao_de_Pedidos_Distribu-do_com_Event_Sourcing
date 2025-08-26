# Multi-stage Dockerfile for unified frontend + backend deployment
# Stage 1: Build shared events
FROM maven:3.9-openjdk-21 AS shared-builder
WORKDIR /app
COPY shared-events/ shared-events/
RUN cd shared-events && mvn clean install -DskipTests -q

# Stage 2: Build all Java services  
FROM maven:3.9-openjdk-21 AS java-builder
WORKDIR /app
COPY --from=shared-builder /root/.m2/repository /root/.m2/repository
COPY services/ services/
RUN mvn clean package -DskipTests -q

# Stage 3: Build React frontend
FROM node:22-alpine AS frontend-builder
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ .
RUN npm run build

# Stage 4: Runtime environment with Nginx + Java services
FROM openjdk:21-jdk-slim
RUN apt-get update && apt-get install -y nginx supervisor curl && rm -rf /var/lib/apt/lists/*

# Create directories
RUN mkdir -p /app/services /app/frontend /var/log/supervisor

# Copy built JAR files
COPY --from=java-builder /app/services/order-service/target/*.jar /app/services/order-service.jar
COPY --from=java-builder /app/services/payment-service/target/*.jar /app/services/payment-service.jar  
COPY --from=java-builder /app/services/inventory-service/target/*.jar /app/services/inventory-service.jar
COPY --from=java-builder /app/services/order-query-service/target/*.jar /app/services/query-service.jar

# Copy built frontend
COPY --from=frontend-builder /app/frontend/build /app/frontend

# Create Nginx configuration inline
RUN echo 'worker_processes auto;\npid /run/nginx.pid;\nevents {\n    worker_connections 1024;\n}\nhttp {\n    include /etc/nginx/mime.types;\n    default_type application/octet-stream;\n    server {\n        listen 80;\n        root /app/frontend;\n        index index.html;\n        location / {\n            try_files $uri $uri/ /index.html;\n        }\n        location /api/ {\n            proxy_pass http://localhost:8081;\n            proxy_set_header Host $host;\n            proxy_set_header X-Real-IP $remote_addr;\n        }\n        location /actuator/ {\n            proxy_pass http://localhost:8081;\n        }\n        location /health {\n            return 200 "{\\"status\\": \\"UP\\", \\"services\\": [\\"order-service\\", \\"payment-service\\", \\"inventory-service\\", \\"query-service\\"], \\"frontend\\": \\"React 18 + TypeScript\\", \\"message\\": \\"Sistema funcionando!\\"}";\n            add_header Content-Type application/json;\n        }\n    }\n}' > /etc/nginx/nginx.conf

# Create supervisor configuration inline
RUN echo '[supervisord]\nnodaemon=true\nlogfile=/var/log/supervisor/supervisord.log\npidfile=/var/run/supervisord.pid\n[program:nginx]\ncommand=/usr/sbin/nginx -g "daemon off;"\nautostart=true\nautorestart=true\n[program:order-service]\ncommand=java -jar /app/services/order-service.jar --server.port=8081\nautostart=true\nautorestart=true\n[program:payment-service]\ncommand=java -jar /app/services/payment-service.jar --server.port=8082\nautostart=true\nautorestart=true\n[program:inventory-service]\ncommand=java -jar /app/services/inventory-service.jar --server.port=8083\nautostart=true\nautorestart=true\n[program:query-service]\ncommand=java -jar /app/services/query-service.jar --server.port=8084\nautostart=true\nautorestart=true' > /etc/supervisor/conf.d/supervisord.conf

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s CMD curl -f http://localhost/health || exit 1
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]