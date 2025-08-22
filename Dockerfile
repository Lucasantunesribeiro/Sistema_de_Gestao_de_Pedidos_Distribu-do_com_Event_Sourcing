FROM maven:3.8.5-openjdk-17 as build
WORKDIR /app

# Copy apenas o order-service
COPY services/order-service/ services/order-service/

# Build apenas o order-service
RUN cd services/order-service && mvn clean package -DskipTests -q

FROM openjdk:17-jdk-alpine
WORKDIR /app
COPY --from=build /app/services/order-service/target/order-service-1.0.0.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
