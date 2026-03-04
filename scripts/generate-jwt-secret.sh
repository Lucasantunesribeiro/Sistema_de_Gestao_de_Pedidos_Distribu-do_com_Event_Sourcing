#!/bin/bash

# Generate JWT Secret Script
# Generates a secure 32-byte base64-encoded secret for JWT tokens

set -e

echo "========================================="
echo "JWT Secret Generator"
echo "========================================="
echo ""

# Generate 32 random bytes and encode to base64
SECRET=$(openssl rand -base64 32 | tr -d '\n')

echo "✓ Generated secure JWT secret (32 bytes, base64 encoded)"
echo ""
echo "Add this to your environment variables (.env file or system environment):"
echo ""
echo "JWT_SECRET_KEY=$SECRET"
echo ""
echo "========================================="
echo "IMPORTANT SECURITY NOTES:"
echo "========================================="
echo "1. Keep this secret secure and never commit it to version control"
echo "2. Use different secrets for development, staging, and production"
echo "3. Rotate secrets periodically (recommended: every 90 days)"
echo "4. Store in environment variables or secure secret management systems"
echo ""
