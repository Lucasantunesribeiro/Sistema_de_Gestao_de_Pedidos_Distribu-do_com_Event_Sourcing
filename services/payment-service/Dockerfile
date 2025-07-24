FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy shared-events first and install it locally
COPY shared-events ./shared-events
RUN apt-get update && \
    apt-get install -y maven && \
    cd shared-events && \
    mvn clean install -DskipTests && \
    cd ..

# Copy payment-service files
COPY services/payment-service/pom.xml .
COPY services/payment-service/src ./src

# Build payment-service
RUN mvn clean package -DskipTests && \
    apt-get remove -y maven && \
    apt-get autoremove -y && \
    rm -rf /var/lib/apt/lists/*

EXPOSE 8083

CMD ["java", "-jar", "target/payment-service-1.0.0.jar"]