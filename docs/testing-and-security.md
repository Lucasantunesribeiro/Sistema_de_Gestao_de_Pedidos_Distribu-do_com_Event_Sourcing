# Testing and Security Documentation

## 🧪 Testing Strategy

### Unit Tests
- **Location:** `services/*/src/test/java/**/*Test.java`
- **Framework:** JUnit 5 + Mockito
- **Execution:** `./scripts/run-tests.sh unit`
- **Dependencies:** None (mocked)

### Integration Tests  
- **Dependencies:** PostgreSQL + Redis
- **Execution:** `./scripts/run-tests.sh integration`
- **Setup:** `docker-compose up -d postgres redis`

### Fixed Issues
- **UnnecessaryStubbingException:** Resolved with `lenient()` stubs
- **Test Configuration:** Profile-based test separation

## 🔐 Security Configuration

### Development Mode (dev/test/local)
```java
@Profile({"dev", "test", "local"})
// All requests permitted - no authentication required
```

### Production Mode (render/prod)
```java
@Profile({"render", "prod"})  
// Protected endpoints with public health checks
```

### Public Endpoints
- `/actuator/**` - Health and monitoring
- `/api/**` - Application APIs  
- `/health` - Simple health check
- `/h2-console/**` - Database console (dev only)

### Environment Variables
```bash
# Development
SPRING_PROFILES_ACTIVE=local

# Production (optional auth)
SPRING_PROFILES_ACTIVE=render
SPRING_SECURITY_USER=admin
SPRING_SECURITY_PASSWORD=<secure-password>
```

## 🚀 CI/CD Pipeline

### Pipeline Stages
1. **unit-tests** - Fast feedback (2-3 min)
2. **integration-tests** - Full validation (5-8 min)
3. **deploy** - Render deployment (10-15 min)
4. **smoke-tests** - Production validation (5-10 min)
5. **rollback** - Automatic on failure

### Health Check URLs
- Order Service: `https://order-service.onrender.com/actuator/health`
- Payment Service: `https://payment-service.onrender.com/actuator/health`
- Inventory Service: `https://inventory-service.onrender.com/actuator/health`
- Query Service: `https://order-query-service.onrender.com/actuator/health`
- Frontend: `https://order-management-frontend.onrender.com`

## 📝 Local Development

### Quick Start
```bash
# Run unit tests
./scripts/run-tests.sh unit

# Check production health  
./scripts/check-health.sh render

# Start local development
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

### Troubleshooting
- **403 Errors:** Check `SPRING_PROFILES_ACTIVE` setting
- **Test Failures:** Ensure shared-events is built first
- **Health Checks:** Verify services are deployed and running