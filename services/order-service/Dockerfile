FROM openjdk:17-jre-slim
WORKDIR /app
COPY target/order-service-1.0.0.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
