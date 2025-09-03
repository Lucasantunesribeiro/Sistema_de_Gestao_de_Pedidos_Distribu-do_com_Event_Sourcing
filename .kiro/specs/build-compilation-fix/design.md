# Design Document

## Overview

This design addresses the compilation errors in the unified order system by creating a comprehensive DTO layer, properly configuring Spring Boot controllers, and aligning service interfaces with test expectations. The solution focuses on maintaining backward compatibility while introducing proper type safety and Spring Boot integration.

## Architecture

### Component Structure
```
unified-order-system/
├── src/main/java/com/ordersystem/unified/
│   ├── order/
│   │   ├── dto/
│   │   │   ├── CreateOrderRequest.java (✓ exists)
│   │   │   ├── OrderResponse.java (✓ exists)
│   │   │   └── OrderItemRequest.java (✓ exists)
│   │   ├── OrderController.java (needs Spring annotations)
│   │   └── OrderService.java (needs DTO integration)
│   ├── payment/
│   │   ├── dto/
│   │   │   ├── PaymentMethod.java (✓ exists, needs BOLETO)
│   │   │   ├── PaymentRequest.java (missing)
│   │   │   ├── PaymentResponse.java (missing)
│   │   │   └── CustomerInfo.java (missing)
│   │   ├── PaymentController.java (needs Spring annotations)
│   │   └── PaymentService.java (needs DTO integration)
│   ├── inventory/
│   │   ├── InventoryController.java (needs Spring annotations)
│   │   └── InventoryService.java (needs method signature fixes)
│   └── config/
│       └── WebConfig.java (new - Spring Boot configuration)
```

### Spring Boot Integration Strategy
- Convert plain controllers to @RestController annotated classes
- Add proper @RequestMapping, @PostMapping, @GetMapping annotations
- Implement @Valid validation for request DTOs
- Add @ResponseStatus annotations for proper HTTP status codes
- Configure proper exception handling with @ControllerAdvice

## Components and Interfaces

### Missing DTO Classes

#### PaymentRequest
```java
public class PaymentRequest {
    private String orderId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private CustomerInfo customerInfo;
    private String correlationId;
    // getters, setters, validation annotations
}
```

#### PaymentResponse  
```java
public class PaymentResponse {
    private String paymentId;
    private String orderId;
    private PaymentStatus status;
    private BigDecimal amount;
    private String transactionId;
    private LocalDateTime processedAt;
    // getters, setters
}
```

#### CustomerInfo
```java
public class CustomerInfo {
    private String customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private Address billingAddress;
    // getters, setters, validation annotations
}
```

### Controller Transformation

#### OrderController Enhancement
- Add @RestController annotation
- Add @RequestMapping("/api/orders") 
- Convert methods to return ResponseEntity<T>
- Add proper HTTP method annotations
- Implement request validation with @Valid
- Add exception handling

#### Service Layer Alignment
- Update InventoryService method signatures to match test expectations
- Add overloaded methods for backward compatibility
- Implement proper DTO conversion methods
- Add correlation ID support throughout the service layer

## Data Models

### Enhanced PaymentMethod Enum
```java
public enum PaymentMethod {
    CREDIT_CARD("Credit Card"),
    DEBIT_CARD("Debit Card"), 
    PAYPAL("PayPal"),
    BANK_TRANSFER("Bank Transfer"),
    CASH("Cash"),
    PIX("PIX"),
    BOLETO("Boleto Bancário");  // Missing value
}
```

### CreateOrderRequest Enhancement
- Add correlationId field and setter method
- Add constructor overloads to match test usage patterns
- Ensure proper validation annotations

## Error Handling

### Validation Strategy
- Use Jakarta Bean Validation (@Valid, @NotNull, @NotBlank, etc.)
- Implement custom validation for business rules
- Return proper HTTP 400 responses for validation failures

### Exception Handling
- Create @ControllerAdvice class for global exception handling
- Map validation exceptions to proper HTTP responses
- Implement proper error response DTOs

### Spring Boot Configuration
- Add @EnableWebMvc configuration
- Configure proper JSON serialization/deserialization
- Set up proper CORS configuration if needed

## Testing Strategy

### Integration Test Fixes
- Ensure MockMvc works with proper @RestController annotations
- Update test assertions to work with ResponseEntity return types
- Fix method signature mismatches in service mocks
- Add proper Spring Boot test configuration

### Performance Test Alignment
- Ensure performance tests use consistent DTO objects
- Update test data creation to use proper constructors
- Align test expectations with actual controller responses

### Unit Test Updates
- Update service layer tests to use proper DTO objects
- Fix mock configurations to match new method signatures
- Ensure proper validation testing

## Implementation Phases

### Phase 1: DTO Creation
- Create missing payment DTOs
- Enhance existing DTOs with missing fields/methods
- Update PaymentMethod enum

### Phase 2: Controller Enhancement  
- Add Spring Boot annotations to all controllers
- Convert return types to ResponseEntity<T>
- Implement proper request mapping

### Phase 3: Service Layer Alignment
- Update InventoryService method signatures
- Add DTO conversion methods
- Implement correlation ID support

### Phase 4: Configuration and Testing
- Add Spring Boot configuration classes
- Fix all test compilation issues
- Ensure end-to-end integration works