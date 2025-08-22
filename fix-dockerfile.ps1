# Fix Dockerfile - Corrigir imagens Docker
Write-Host "Corrigindo Dockerfile com imagens validas..." -ForegroundColor Green

# Criar Dockerfile corrigido na raiz
Write-Host "Criando Dockerfile corrigido..." -ForegroundColor Yellow

$fixedDockerfile = @"
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
"@

$fixedDockerfile | Out-File -FilePath "Dockerfile" -Encoding UTF8
Write-Host "   Dockerfile corrigido criado!" -ForegroundColor Green

# Criar render.yaml simplificado (apenas order-service primeiro)
Write-Host "Criando render.yaml simplificado..." -ForegroundColor Yellow

$simplifiedRender = @"
services:
  - type: web
    name: gestao-pedidos
    env: docker
    dockerfilePath: ./Dockerfile
    healthCheckPath: /actuator/health
    envVars:
      - key: PORT
        value: 8080
      - key: SPRING_PROFILES_ACTIVE
        value: render
      - key: DATABASE_URL
        fromDatabase:
          name: ordersystem-db
          property: connectionString

databases:
  - name: ordersystem-db
    databaseName: ordersystem
    user: ordersystem
"@

$simplifiedRender | Out-File -FilePath "render.yaml" -Encoding UTF8
Write-Host "   render.yaml simplificado criado!" -ForegroundColor Green

# Criar application-render.yml especifico para order-service
Write-Host "Atualizando configuracao render..." -ForegroundColor Yellow

$renderConfig = @"
server:
  port: `${PORT:8080}

spring:
  profiles:
    active: render
  datasource:
    url: `${DATABASE_URL:jdbc:h2:mem:testdb}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 20000
      
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    
  application:
    name: gestao-pedidos

logging:
  level:
    com.ordersystem: INFO
    org.springframework: WARN
    org.hibernate: WARN
    
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
      
# RabbitMQ desabilitado por enquanto
rabbitmq:
  enabled: false
"@

$renderConfig | Out-File -FilePath "services\order-service\src\main\resources\application-render.yml" -Encoding UTF8
Write-Host "   application-render.yml atualizado!" -ForegroundColor Green

# Criar .dockerignore para otimizar build
Write-Host "Criando .dockerignore..." -ForegroundColor Yellow

$dockerignore = @"
# Ignorar arquivos desnecessarios no build
.git
.gitignore
README.md
*.md
.env*
node_modules
target
.idea
.vscode
*.log
.DS_Store
Thumbs.db

# Manter apenas o essencial
!shared-events/
!services/order-service/
!pom.xml
"@

$dockerignore | Out-File -FilePath ".dockerignore" -Encoding UTF8
Write-Host "   .dockerignore criado!" -ForegroundColor Green

Write-Host "`nCorreçoes aplicadas!" -ForegroundColor Green
Write-Host "===================" -ForegroundColor Cyan

Write-Host "`nMudanças feitas:" -ForegroundColor Yellow
Write-Host "✅ Dockerfile com imagem Maven valida" -ForegroundColor White
Write-Host "✅ render.yaml simplificado (apenas 1 service)" -ForegroundColor White
Write-Host "✅ Configuracao PostgreSQL corrigida" -ForegroundColor White
Write-Host "✅ Health check adicionado" -ForegroundColor White
Write-Host "✅ .dockerignore para build otimizado" -ForegroundColor White

Write-Host "`nCommit e push:" -ForegroundColor Yellow
Write-Host "git add ." -ForegroundColor White
Write-Host "git commit -m 'Fix Docker images and simplify deployment'" -ForegroundColor White
Write-Host "git push origin main" -ForegroundColor White

Write-Host "`nDepois Manual Deploy no Render!" -ForegroundColor Green