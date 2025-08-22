# Dockerfile for building shared-events library
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml first for better caching
COPY pom.xml ./

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the library
RUN mvn clean install -DskipTests

# Create a minimal image with the built artifacts
FROM alpine:latest

WORKDIR /output

# Copy the built jar and install it to a local repository structure
COPY --from=builder /root/.m2/repository/com/ordersystem/shared-events ./

# Create a simple script to verify the build
RUN echo '#!/bin/sh' > /verify.sh && \
    echo 'echo "Shared events library built successfully"' >> /verify.sh && \
    echo 'ls -la /output/' >> /verify.sh && \
    chmod +x /verify.sh

CMD ["/verify.sh"]