#!/bin/bash

# Start services script for unified deployment
set -e

echo "Starting Order Management System..."

# Wait for database to be ready
echo "Waiting for database connection..."
until nc -z ${DATABASE_HOST:-localhost} ${DATABASE_PORT:-5432}; do
  echo "Database is unavailable - sleeping"
  sleep 2
done
echo "Database is ready!"

# Wait for RabbitMQ to be ready
echo "Waiting for RabbitMQ connection..."
until nc -z ${RABBITMQ_HOST:-localhost} ${RABBITMQ_PORT:-5672}; do
  echo "RabbitMQ is unavailable - sleeping"
  sleep 2
done
echo "RabbitMQ is ready!"

# Create log directories
mkdir -p /app/logs

# Set environment variables for production
export SPRING_PROFILES_ACTIVE=production
export JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseStringDeduplication"

echo "Environment setup complete. Starting services with Supervisor..."

# Start supervisor which will manage all services
exec /usr/bin/supervisord -c /etc/supervisor/conf.d/supervisord.conf