FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy shared-events first and install it locally
COPY shared-events ./shared-events
RUN apt-get update && \
    apt-get install -y maven && \
    cd shared-events && \
    mvn clean install -DskipTests && \
    cd ..

# Copy order-query-service files
COPY services/order-query-service/pom.xml .
COPY services/order-query-service/src ./src

# Build order-query-service
RUN mvn clean package -DskipTests && \
    apt-get remove -y maven && \
    apt-get autoremove -y && \
    rm -rf /var/lib/apt/lists/*

EXPOSE 8084

CMD ["java", "-jar", "target/order-query-service-1.0.0.jar"]