#!/usr/bin/env bash
set -euo pipefail
BASE_URL=${1:-http://localhost:8080}

echo "Checking health..."
curl -i "$BASE_URL/actuator/health"

echo "Checking orders..."
curl -i "$BASE_URL/api/orders"
