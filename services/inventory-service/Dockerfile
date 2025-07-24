FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy shared-events first and install it locally
COPY shared-events ./shared-events
RUN apt-get update && \
    apt-get install -y maven && \
    cd shared-events && \
    mvn clean install -DskipTests && \
    cd ..

# Copy inventory-service files
COPY services/inventory-service/pom.xml .
COPY services/inventory-service/src ./src

# Build inventory-service
RUN mvn clean package -DskipTests && \
    apt-get remove -y maven && \
    apt-get autoremove -y && \
    rm -rf /var/lib/apt/lists/*

EXPOSE 8082

CMD ["java", "-jar", "target/inventory-service-1.0.0.jar"]