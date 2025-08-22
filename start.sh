#!/bin/bash
SERVICE_NAME=${1:-order-service}
echo "Starting $SERVICE_NAME..."
java -jar $SERVICE_NAME-1.0.0.jar
