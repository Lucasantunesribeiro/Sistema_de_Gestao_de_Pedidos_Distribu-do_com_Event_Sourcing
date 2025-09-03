# Design Document

## Overview

O sistema atual possui todas as entidades JPA e repositórios necessários para persistência no banco PostgreSQL, mas o OrderController está usando armazenamento em memória como fallback. O problema é que o controller não está utilizando corretamente o OrderService que já implementa persistência real. A solução é modificar o OrderController para usar consistentemente o OrderService e remover o armazenamento em memória temporário.

## Architecture

### Current State Analysis
- ✅ **Entidades JPA**: Order e OrderItemEntity já existem e estão corretamente mapeadas
- ✅ **Repository**: OrderRepository já implementado com queries necessárias  
- ✅ **Service Layer**: OrderService já implementa persistência completa
- ❌ **Controller Layer**: Usando armazenamento em memória ao invés do service
- ❌ **Data Flow**: POST não salva no banco, GET não lê do banco

### Target Architecture
```
Frontend Request → OrderController → OrderService → OrderRepository → PostgreSQL Database
                                                                    ↓
Frontend Response ← OrderController ← OrderService ← OrderRepository ← PostgreSQL Database
```

## Components and Interfaces

### 1. OrderController Modifications

**Current Issues:**
- `handleSimplifiedOrder()` cria resposta mock sem salvar no banco
- `getOrders()` usa dados mock ao invés do OrderService
- Lista estática `CREATED_ORDERS` em memória que se perde no restart

**Required Changes:**
- Remover armazenamento em memória (`CREATED_ORDERS`)
- Modificar `handleSimplifiedOrder()` para usar `OrderService.createOrder()`
- Modificar `getOrders()` para sempre usar `OrderService.getRecentOrders()`
- Implementar conversão correta entre formato simplificado e `CreateOrderRequest`

### 2. Data Conversion Layer

**SimpleOrderRequest → CreateOrderRequest Mapping:**
```java
// Input (Frontend)
{
  "customerName": "João Silva",
  "items": [{"productName": "Notebook", "price": 2500, "quantity": 1}],
  "totalAmount": 2500
}

// Conversion to CreateOrderRequest
CreateOrderRequest {
  customerId: UUID.randomUUID().toString(), // Generate if not provided
  customerName: "João Silva",
  items: [OrderItemRequest {
    productId: UUID.randomUUID().toString(), // Generate if not provided
    productName: "Notebook",
    quantity: 1,
    unitPrice: BigDecimal.valueOf(2500)
  }]
}
```

### 3. Error Handling Strategy

**Database Connection Issues:**
- Catch specific database exceptions
- Return appropriate HTTP status codes (500 for DB errors, not 201 fake success)
- Log detailed error information for debugging

**Validation Errors:**
- Validate input data before calling service
- Return 400 Bad Request for invalid data
- Provide clear error messages to frontend

## Data Models

### Existing Entities (No Changes Needed)

**Order Entity:**
- ✅ Correctly mapped to `orders` table
- ✅ Proper relationships with OrderItemEntity
- ✅ Audit fields (createdAt, updatedAt)
- ✅ Status enum handling

**OrderItemEntity:**
- ✅ Correctly mapped to `order_items` table  
- ✅ Proper foreign key relationship
- ✅ Price and quantity validation

### Database Schema (Already Exists)
```sql
-- Tables are auto-created by JPA
CREATE TABLE orders (
    id VARCHAR(255) PRIMARY KEY,
    customer_id VARCHAR(255) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    correlation_id VARCHAR(255)
);

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(255) NOT NULL REFERENCES orders(id),
    product_id VARCHAR(255) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    price DECIMAL(10,2) NOT NULL
);
```

## Error Handling

### Database Error Scenarios

1. **Connection Timeout:**
   - Log: "Database connection timeout"
   - Response: 500 Internal Server Error
   - Action: Retry logic or circuit breaker

2. **Constraint Violation:**
   - Log: "Data validation failed: {details}"
   - Response: 400 Bad Request
   - Action: Return validation error details

3. **Transaction Rollback:**
   - Log: "Transaction failed, rolling back"
   - Response: 500 Internal Server Error  
   - Action: Ensure data consistency

### Input Validation Errors

1. **Missing Required Fields:**
   - Response: 400 Bad Request
   - Message: "Field {fieldName} is required"

2. **Invalid Data Types:**
   - Response: 400 Bad Request
   - Message: "Invalid value for {fieldName}"

## Testing Strategy

### Unit Tests

1. **OrderController Tests:**
   - Test simplified order creation with valid data
   - Test order retrieval with various filters
   - Test error handling for invalid input
   - Mock OrderService to isolate controller logic

2. **Integration Tests:**
   - Test complete flow: POST → Database → GET
   - Test with real PostgreSQL database
   - Verify data persistence across application restarts
   - Test concurrent order creation

3. **Database Tests:**
   - Test entity mapping and relationships
   - Test repository queries
   - Test transaction handling

### Test Scenarios

**Scenario 1: Create and Retrieve Order**
```java
@Test
public void testCreateAndRetrieveOrder() {
    // 1. POST order via controller
    // 2. Verify 201 response with order ID
    // 3. GET orders via controller  
    // 4. Verify order appears in list
    // 5. Verify all data matches
}
```

**Scenario 2: Application Restart Persistence**
```java
@Test
public void testOrderPersistenceAfterRestart() {
    // 1. Create order
    // 2. Simulate application restart (clear caches)
    // 3. Retrieve orders
    // 4. Verify order still exists
}
```

**Scenario 3: Concurrent Order Creation**
```java
@Test
public void testConcurrentOrderCreation() {
    // 1. Create multiple orders simultaneously
    // 2. Verify all orders are saved
    // 3. Verify no data corruption
    // 4. Verify correct order count
}
```

### Performance Tests

1. **Load Testing:**
   - Test with 100+ concurrent order creations
   - Measure response times
   - Monitor database connection pool

2. **Memory Usage:**
   - Verify no memory leaks after removing in-memory storage
   - Monitor JVM heap usage during high load

## Implementation Notes

### Critical Changes Required

1. **Remove In-Memory Storage:**
   ```java
   // REMOVE this line from OrderController
   private static final List<Map<String, Object>> CREATED_ORDERS = ...;
   ```

2. **Fix handleSimplifiedOrder Method:**
   ```java
   // CHANGE from mock response to real service call
   CreateOrderRequest request = convertToCreateOrderRequest(requestMap);
   OrderResponse response = orderService.createOrder(request);
   return ResponseEntity.status(HttpStatus.CREATED).body(response);
   ```

3. **Fix getOrders Method:**
   ```java
   // CHANGE from mock data to real service call
   List<OrderResponse> orders = orderService.getRecentOrders(pageable);
   return ResponseEntity.ok(orders);
   ```

### Data Conversion Utilities

**Required Helper Methods:**
- `convertToCreateOrderRequest(Map<String, Object> simplifiedRequest)`
- `generateCustomerId()` - for cases where customer ID is not provided
- `generateProductId()` - for cases where product ID is not provided
- `validateSimplifiedRequest(Map<String, Object> request)`

### Logging Strategy

**Key Log Points:**
- Order creation start/success/failure
- Database operation timing
- Data conversion steps
- Error details with correlation IDs

**Log Levels:**
- INFO: Successful operations, business events
- WARN: Recoverable errors, fallback usage
- ERROR: Unrecoverable errors, system failures
- DEBUG: Detailed flow information, data dumps

## Migration Strategy

### Phase 1: Remove In-Memory Storage
- Remove static CREATED_ORDERS list
- Update imports to remove unused collections

### Phase 2: Implement Service Integration  
- Modify handleSimplifiedOrder to use OrderService
- Implement data conversion utilities
- Add proper error handling

### Phase 3: Update GET Operations
- Modify getOrders to use OrderService consistently
- Remove mock data fallbacks
- Ensure proper pagination handling

### Phase 4: Testing and Validation
- Run integration tests
- Verify data persistence
- Test error scenarios
- Performance validation

## Success Criteria

1. **Functional Requirements:**
   - ✅ Orders created via POST are saved to PostgreSQL
   - ✅ Orders retrieved via GET come from PostgreSQL
   - ✅ Data persists across application restarts
   - ✅ No more mock/fake responses

2. **Non-Functional Requirements:**
   - ✅ Response times under 2 seconds for order operations
   - ✅ Support for 100+ concurrent users
   - ✅ Proper error handling and logging
   - ✅ Zero data loss during normal operations

3. **Technical Requirements:**
   - ✅ Remove all in-memory storage mechanisms
   - ✅ Use existing JPA entities and repositories
   - ✅ Maintain API compatibility
   - ✅ Comprehensive error handling