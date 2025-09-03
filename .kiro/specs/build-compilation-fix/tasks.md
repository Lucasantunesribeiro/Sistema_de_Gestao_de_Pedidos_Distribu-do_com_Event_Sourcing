# Implementation Plan

- [x] 1. Create missing payment DTO classes


  - Create PaymentRequest, PaymentResponse, and CustomerInfo DTO classes with proper validation annotations
  - Add missing BOLETO value to PaymentMethod enum
  - _Requirements: 2.1, 2.2, 2.3, 2.4_





- [x] 2. Enhance CreateOrderRequest DTO class


  - Add correlationId field with getter and setter methods
  - Add constructor overloads to match test usage patterns


  - Ensure proper validation annotations are in place


  - _Requirements: 2.5_

- [ ] 3. Transform OrderController to proper Spring Boot REST controller
  - Add @RestController and @RequestMapping annotations




  - Convert method return types to ResponseEntity<T>
  - Add proper HTTP method annotations (@PostMapping, @GetMapping, etc.)
  - Implement request validation with @Valid annotations


  - _Requirements: 3.1, 3.2, 3.4_





- [ ] 4. Update PaymentController with Spring Boot annotations
  - Add @RestController and @RequestMapping annotations
  - Convert methods to use proper DTO objects instead of Map<String, Object>
  - Add proper HTTP method annotations and response status codes




  - _Requirements: 3.1, 3.2, 3.4_



- [ ] 5. Fix InventoryService method signatures
  - Update reserveItems method to support both reservation ID and item list parameters




  - Add missing methods expected by tests (releaseItems, confirmReservation, etc.)
  - Ensure method signatures match test expectations
  - Add proper DTO return types instead of Map<String, Object>
  - _Requirements: 4.1, 4.2, 4.3_





- [ ] 6. Update InventoryController with Spring Boot integration
  - Add @RestController and @RequestMapping annotations



  - Convert methods to use proper DTO objects


  - Add proper HTTP method annotations
  - _Requirements: 3.1, 3.2, 3.4_

- [ ] 7. Create Spring Boot configuration classes


  - Create WebConfig class for Spring Boot web configuration
  - Add @EnableWebMvc and other necessary configurations
  - Ensure proper JSON serialization/deserialization setup
  - _Requirements: 3.3_

- [ ] 8. Implement global exception handling
  - Create @ControllerAdvice class for global exception handling
  - Map validation exceptions to proper HTTP 400 responses
  - Create proper error response DTO classes
  - _Requirements: 6.1, 6.2, 6.3, 6.4_

- [ ] 9. Fix integration test compilation issues
  - Update PaymentServiceIntegrationTest to use proper DTO classes
  - Fix InventoryServiceIntegrationTest method call issues (andExpected -> andExpect)
  - Ensure all test classes compile without symbol errors
  - _Requirements: 5.1, 5.2_

- [ ] 10. Fix unit test compilation issues
  - Update OrderControllerTest to use proper DTO constructors and method signatures
  - Fix OrderServiceTest to use proper service method signatures
  - Update InventoryServiceTest to match new service interface
  - Ensure proper mock configurations for new DTO return types
  - _Requirements: 5.1, 5.3, 5.4_

- [ ] 11. Update performance test to use consistent DTOs
  - Ensure PerformanceIntegrationTest uses proper CreateOrderRequest constructor
  - Update test assertions to work with proper DTO return types
  - Verify all performance tests compile and run successfully
  - _Requirements: 5.1, 5.4_

- [ ] 12. Verify complete build and test execution
  - Run Maven compile to ensure no compilation errors
  - Execute all unit tests to verify they pass
  - Run integration tests to ensure proper Spring Boot integration
  - Validate that the application starts successfully
  - _Requirements: 1.1, 1.2, 1.3_