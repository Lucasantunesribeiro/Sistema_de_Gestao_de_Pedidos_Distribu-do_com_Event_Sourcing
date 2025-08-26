# Order Management System - Troubleshooting Guide

## Common Issues and Solutions

### 1. Query Service 500 Error

**Symptoms:**
- Frontend shows "Failed to fetch orders"
- Query Service returns HTTP 500 errors
- Logs show database connection issues or event processing errors

**Solutions:**

#### Check Database Connection
```bash
# Test database connectivity
docker exec -it <postgres-container> psql -U orderuser -d orderdb -c "SELECT 1;"
```

#### Check RabbitMQ Connection
```bash
# Check RabbitMQ management interface
curl http://localhost:15672/api/overview
# Default credentials: orderuser/orderpass
```

#### Check Service Logs
```bash
# Docker Compose logs
docker-compose logs query-service

# Unified deployment logs
docker logs order-management-system
```

#### Verify Event Processing
```bash
# Check RabbitMQ queues
curl -u orderuser:orderpass http://localhost:15672/api/queues
```

### 2. Service Startup Issues

**Symptoms:**
- Services fail to start
- Connection refused errors
- Timeout errors during startup

**Solutions:**

#### Check Service Dependencies
```bash
# Ensure database is running
docker-compose ps postgres

# Ensure RabbitMQ is running
docker-compose ps rabbitmq
```

#### Check Port Conflicts
```bash
# Windows
netstat -an | findstr :8081
netstat -an | findstr :8082
netstat -an | findstr :8083
netstat -an | findstr :8084

# Linux/Mac
netstat -tulpn | grep :808
```

#### Increase Startup Timeout
```yaml
# In docker-compose.yml, add healthcheck
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 5
  start_period: 60s
```

### 3. Frontend API Connection Issues

**Symptoms:**
- Frontend shows "Network Error"
- CORS errors in browser console
- API requests timing out

**Solutions:**

#### Check API Base URL
```javascript
// In frontend/src/services/api.js
const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8084';
```

#### Verify CORS Configuration
```java
// In SecurityConfig.java
configuration.setAllowedOriginPatterns(Arrays.asList("*"));
configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
```

#### Test API Directly
```bash
# Test health endpoint
curl http://localhost:8084/actuator/health

# Test orders endpoint
curl http://localhost:8084/api/orders
```

### 4. Database Issues

**Symptoms:**
- "Connection refused" errors
- "Database does not exist" errors
- Data not persisting

**Solutions:**

#### Reset Database
```bash
# Stop services
docker-compose down

# Remove volumes
docker-compose down -v

# Restart
docker-compose up -d
```

#### Check Database Schema
```sql
-- Connect to database
\c orderdb

-- List tables
\dt

-- Check specific table
SELECT * FROM order_view LIMIT 5;
```

#### Manual Database Setup
```sql
-- Create database if needed
CREATE DATABASE orderdb;
CREATE USER orderuser WITH PASSWORD 'orderpass';
GRANT ALL PRIVILEGES ON DATABASE orderdb TO orderuser;
```

### 5. RabbitMQ Message Issues

**Symptoms:**
- Events not being processed
- Messages stuck in queues
- Event listeners not triggering

**Solutions:**

#### Check Queue Status
```bash
# List all queues
curl -u orderuser:orderpass http://localhost:15672/api/queues

# Check specific queue
curl -u orderuser:orderpass http://localhost:15672/api/queues/%2F/order.created.queue
```

#### Purge Stuck Messages
```bash
# Purge queue via management interface
curl -u orderuser:orderpass -X DELETE http://localhost:15672/api/queues/%2F/order.created.queue/contents
```

#### Restart RabbitMQ
```bash
docker-compose restart rabbitmq
```

### 6. Performance Issues

**Symptoms:**
- Slow API responses
- High memory usage
- Timeouts

**Solutions:**

#### Check Resource Usage
```bash
# Docker stats
docker stats

# Service-specific stats
docker stats order-service payment-service inventory-service query-service
```

#### Optimize JVM Settings
```bash
# In docker-compose.yml or environment files
JAVA_OPTS=-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseStringDeduplication
```

#### Database Query Optimization
```sql
-- Check slow queries
SELECT query, mean_time, calls 
FROM pg_stat_statements 
ORDER BY mean_time DESC 
LIMIT 10;

-- Add indexes if needed
CREATE INDEX idx_order_view_customer_id ON order_view(customer_id);
```

### 7. Build Issues

**Symptoms:**
- Maven build failures
- NPM install errors
- Docker build failures

**Solutions:**

#### Clean Build
```bash
# Clean Maven
mvn clean

# Clean NPM
cd frontend && npm ci

# Clean Docker
docker system prune -f
```

#### Check Java Version
```bash
java -version
# Should be Java 17
```

#### Check Node Version
```bash
node --version
npm --version
# Node 18+ recommended
```

### 8. Deployment Issues

**Symptoms:**
- Services not accessible
- Load balancer errors
- SSL/TLS issues

**Solutions:**

#### Check Nginx Configuration
```bash
# Test nginx config
nginx -t

# Reload nginx
nginx -s reload
```

#### Check Service Discovery
```bash
# Test upstream services
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
```

#### Check Firewall/Network
```bash
# Test port accessibility
telnet localhost 8081
telnet localhost 8084
```

## Diagnostic Commands

### Health Check All Services
```bash
# PowerShell script
.\scripts\test-integration.ps1

# Manual checks
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health  
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
```

### Log Analysis
```bash
# View all service logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f query-service

# Search for errors
docker-compose logs | grep -i error
docker-compose logs | grep -i exception
```

### Database Diagnostics
```sql
-- Check active connections
SELECT * FROM pg_stat_activity WHERE datname = 'orderdb';

-- Check table sizes
SELECT schemaname,tablename,attname,n_distinct,correlation 
FROM pg_stats 
WHERE schemaname = 'public';

-- Check recent activity
SELECT * FROM order_view ORDER BY created_at DESC LIMIT 10;
```

### RabbitMQ Diagnostics
```bash
# Check cluster status
curl -u orderuser:orderpass http://localhost:15672/api/cluster-name

# Check exchanges
curl -u orderuser:orderpass http://localhost:15672/api/exchanges

# Check bindings
curl -u orderuser:orderpass http://localhost:15672/api/bindings
```

## Emergency Procedures

### Complete System Reset
```bash
# Stop everything
docker-compose down -v

# Clean Docker
docker system prune -f
docker volume prune -f

# Rebuild and restart
.\scripts\build.ps1
.\scripts\deploy.ps1
```

### Backup and Restore
```bash
# Backup database
docker exec postgres pg_dump -U orderuser orderdb > backup.sql

# Restore database
docker exec -i postgres psql -U orderuser orderdb < backup.sql
```

### Service Recovery
```bash
# Restart specific service
docker-compose restart query-service

# Scale service (if needed)
docker-compose up -d --scale query-service=2
```

## Monitoring and Alerting

### Key Metrics to Monitor
- Service health endpoints
- Database connection pool usage
- RabbitMQ queue lengths
- API response times
- Error rates

### Log Patterns to Watch
- `ERROR` level messages
- `Connection refused` errors
- `Timeout` errors
- `OutOfMemoryError`
- `SQLException`

### Performance Thresholds
- API response time > 5 seconds
- Database connection pool > 80% usage
- RabbitMQ queue length > 1000 messages
- JVM heap usage > 85%

## Getting Help

1. Check this troubleshooting guide
2. Review service logs for specific error messages
3. Test individual components in isolation
4. Check system resources (CPU, memory, disk)
5. Verify network connectivity between services
6. Consult the application architecture documentation

For additional support, include the following information:
- Error messages and stack traces
- Service logs
- System configuration
- Steps to reproduce the issue
- Expected vs actual behavior