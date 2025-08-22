#!/bin/bash

# Configurações de ambiente para Render
export SPRING_PROFILES_ACTIVE=render

# Configurações H2 para evitar PostgreSQL
export DATABASE_URL=${DATABASE_URL:-"jdbc:h2:mem:orderdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"}
export DATABASE_USERNAME=${DATABASE_USERNAME:-"sa"}
export DATABASE_PASSWORD=${DATABASE_PASSWORD:-"password"}

# Configurações RabbitMQ (disabled for Render)
export RABBITMQ_ENABLED=${RABBITMQ_ENABLED:-"false"}
export RABBITMQ_HOST=${RABBITMQ_HOST:-"localhost"}

# Configurações Redis (disabled for Render)
export REDIS_ENABLED=${REDIS_ENABLED:-"false"}
export REDIS_HOST=${REDIS_HOST:-"localhost"}

echo "Starting Order Management System on Render..."

# Start Order Service on primary port (8080 for Render)
echo "Starting Order Service on port 8080..."
java -Xmx256m -jar order-service.jar --server.port=8080 &
ORDER_PID=$!

# Wait for Order Service to be ready
sleep 10

# Start other services on different ports (background)
echo "Starting Payment Service on port 8082..."
java -Xmx128m -jar payment-service.jar --server.port=8082 &
PAYMENT_PID=$!

echo "Starting Inventory Service on port 8083..."
java -Xmx128m -jar inventory-service.jar --server.port=8083 &
INVENTORY_PID=$!

echo "Starting Query Service on port 8084..."
java -Xmx128m -jar query-service.jar --server.port=8084 &
QUERY_PID=$!

# Health check function
check_health() {
    local port=$1
    local service=$2
    curl -f http://localhost:$port/health 2>/dev/null
    if [ $? -eq 0 ]; then
        echo "$service is healthy"
        return 0
    else
        echo "$service is not responding"
        return 1
    fi
}

# Wait for all services to be healthy
echo "Waiting for services to start..."
sleep 15

echo "Checking service health..."
check_health 8080 "Order Service"
check_health 8082 "Payment Service" 
check_health 8083 "Inventory Service"
check_health 8084 "Query Service"

echo "All services started successfully!"
echo "Order Service (Primary): http://localhost:8080"
echo "Payment Service: http://localhost:8082"
echo "Inventory Service: http://localhost:8083" 
echo "Query Service: http://localhost:8084"

# Function to handle shutdown
shutdown() {
    echo "Shutting down services..."
    kill $ORDER_PID $PAYMENT_PID $INVENTORY_PID $QUERY_PID 2>/dev/null
    wait $ORDER_PID $PAYMENT_PID $INVENTORY_PID $QUERY_PID 2>/dev/null
    echo "All services stopped"
    exit 0
}

# Set trap to handle SIGTERM and SIGINT
trap shutdown SIGTERM SIGINT

# Wait for the primary service (Order Service)
wait $ORDER_PID