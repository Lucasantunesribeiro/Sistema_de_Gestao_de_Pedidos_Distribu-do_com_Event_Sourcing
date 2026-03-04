# Multi-stage build for unified order system
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy build files
COPY pom.xml .
COPY libs ./libs
COPY shared-events ./shared-events
COPY unified-order-system ./unified-order-system

# Install shared libs, then package the application
RUN mvn -pl libs/common-events,libs/common-security,libs/common-messaging,libs/common-observability,shared-events -am -DskipTests install && \
    mvn -f unified-order-system/pom.xml -DskipTests package

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache curl && \
    addgroup -g 1001 -S appuser && adduser -u 1001 -S appuser -G appuser

WORKDIR /app

COPY --from=build /app/unified-order-system/target/unified-order-system-1.0.0.jar app.jar

RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=200", "-XX:+UseContainerSupport", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
