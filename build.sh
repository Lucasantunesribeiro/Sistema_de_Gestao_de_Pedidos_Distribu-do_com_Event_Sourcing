#!/bin/bash
set -e
echo "Building Sistema de Gestao de Pedidos..."

echo "1. Building shared-events..."
cd shared-events
mvn clean install -DskipTests -q
cd ..

echo "2. Building services..."
cd services/order-service
mvn clean package -DskipTests -q
cd ../..

cd services/payment-service  
mvn clean package -DskipTests -q
cd ../..

cd services/inventory-service
mvn clean package -DskipTests -q
cd ../..

cd services/order-query-service
mvn clean package -DskipTests -q
cd ../..

echo "Build completed successfully!"
