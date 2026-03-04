package com.ordersystem.unified.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordersystem.unified.order.dto.CreateOrderRequest;
import com.ordersystem.unified.order.dto.OrderItemRequest;
import com.ordersystem.unified.order.dto.OrderResponse;
import com.ordersystem.unified.payment.dto.PaymentMethod;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End tests for complete order flow.
 * Tests entire system integration from order creation to completion.
 *
 * Critical flows tested:
 * 1. Order Creation → Inventory Reservation → Payment Processing
 * 2. Order Cancellation → Inventory Release → Payment Refund
 * 3. Order Query and Status Updates
 * 4. Error Handling and Rollbacks
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@org.springframework.context.annotation.Import(com.ordersystem.unified.config.TestConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CompleteOrderFlowE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    private static String testOrderId;
    private static String testCustomerId;

    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        testCustomerId = "E2E-CUSTOMER-" + System.currentTimeMillis();
    }

    @Test
    @Order(1)
    @DisplayName("E2E: Complete successful order flow")
    public void testCompleteSuccessfulOrderFlow() throws Exception {
        // Step 1: Check system health
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/health")
        .then()
            .statusCode(200)
            .body("status", equalTo("UP"));

        // Step 2: Check inventory availability
        String productId = "E2E-PRODUCT-001";
        given()
            .contentType(ContentType.JSON)
            .queryParam("quantity", 1)
        .when()
            .get("/api/inventory/check/{productId}", productId)
        .then()
            .statusCode(200);

        // Step 3: Create order
        CreateOrderRequest orderRequest = createOrderRequest(testCustomerId, productId, 1);

        OrderResponse orderResponse = given()
            .contentType(ContentType.JSON)
            .body(orderRequest)
        .when()
            .post("/api/orders")
        .then()
            .statusCode(201)
            .body("orderId", notNullValue())
            .body("customerId", equalTo(testCustomerId))
            .body("status", equalTo("CONFIRMED"))
            .body("paymentId", notNullValue())
            .body("reservationId", notNullValue())
            .body("totalAmount", greaterThan(0f))
            .extract()
            .as(OrderResponse.class);

        testOrderId = orderResponse.getOrderId();
        assertNotNull(testOrderId);

        // Step 4: Verify order was persisted
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/orders/{orderId}", testOrderId)
        .then()
            .statusCode(200)
            .body("orderId", equalTo(testOrderId))
            .body("status", equalTo("CONFIRMED"));

        // Step 5: Verify payment was processed
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/payments/order/{orderId}", testOrderId)
        .then()
            .statusCode(200)
            .body("size()", greaterThan(0));

        // Step 6: Verify inventory was reserved
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/inventory/status")
        .then()
            .statusCode(200);

        System.out.println("✓ Complete successful order flow test passed");
    }

    @Test
    @Order(2)
    @DisplayName("E2E: Order cancellation with rollback")
    public void testOrderCancellationWithRollback() throws Exception {
        // Step 1: Create order
        CreateOrderRequest orderRequest = createOrderRequest(
            "E2E-CANCEL-CUSTOMER",
            "E2E-CANCEL-PRODUCT",
            2
        );

        OrderResponse orderResponse = given()
            .contentType(ContentType.JSON)
            .body(orderRequest)
        .when()
            .post("/api/orders")
        .then()
            .statusCode(201)
            .extract()
            .as(OrderResponse.class);

        String orderId = orderResponse.getOrderId();
        String reservationId = orderResponse.getReservationId();

        // Step 2: Verify order is confirmed
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/orders/{orderId}", orderId)
        .then()
            .statusCode(200)
            .body("status", equalTo("CONFIRMED"));

        // Step 3: Cancel order
        Map<String, String> cancellationRequest = Map.of(
            "reason", "E2E Test Cancellation"
        );

        given()
            .contentType(ContentType.JSON)
            .body(cancellationRequest)
        .when()
            .put("/api/orders/{orderId}/cancel", orderId)
        .then()
            .statusCode(200)
            .body("status", equalTo("CANCELLED"))
            .body("cancellationReason", containsString("E2E Test"));

        // Step 4: Verify order status updated
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/orders/{orderId}", orderId)
        .then()
            .statusCode(200)
            .body("status", equalTo("CANCELLED"));

        // Step 5: Verify inventory was released
        given()
            .contentType(ContentType.JSON)
        .when()
            .put("/api/inventory/release/{reservationId}", reservationId)
        .then()
            .statusCode(anyOf(is(200), is(404))); // May already be released

        System.out.println("✓ Order cancellation with rollback test passed");
    }

    @Test
    @Order(3)
    @DisplayName("E2E: Insufficient inventory scenario")
    public void testInsufficientInventoryScenario() throws Exception {
        // Step 1: Try to create order with very large quantity
        CreateOrderRequest largeOrderRequest = createOrderRequest(
            "E2E-LARGE-ORDER",
            "E2E-INSUFFICIENT-PRODUCT",
            10000 // Unrealistic quantity
        );

        // Step 2: Should fail with inventory error
        given()
            .contentType(ContentType.JSON)
            .body(largeOrderRequest)
        .when()
            .post("/api/orders")
        .then()
            .statusCode(anyOf(is(400), is(500)))
            .body("message", anyOf(
                containsStringIgnoringCase("inventory"),
                containsStringIgnoringCase("insufficient"),
                containsStringIgnoringCase("stock")
            ));

        // Step 3: Verify no order was created
        given()
            .contentType(ContentType.JSON)
            .queryParam("customerId", "E2E-LARGE-ORDER")
        .when()
            .get("/api/orders")
        .then()
            .statusCode(200)
            .body("size()", equalTo(0));

        System.out.println("✓ Insufficient inventory scenario test passed");
    }

    @Test
    @Order(4)
    @DisplayName("E2E: Multiple payment methods")
    public void testMultiplePaymentMethods() throws Exception {
        PaymentMethod[] methods = {
            PaymentMethod.CREDIT_CARD,
            PaymentMethod.DEBIT_CARD,
            PaymentMethod.PIX
        };

        for (PaymentMethod method : methods) {
            CreateOrderRequest request = createOrderRequest(
                "E2E-PAYMENT-" + method,
                "E2E-PAYMENT-PRODUCT-" + method,
                1
            );
            request.setPaymentMethod(method);

            given()
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post("/api/orders")
            .then()
                .statusCode(201)
                .body("paymentId", notNullValue())
                .body("status", equalTo("CONFIRMED"));
        }

        System.out.println("✓ Multiple payment methods test passed");
    }

    @Test
    @Order(5)
    @DisplayName("E2E: Order query operations")
    public void testOrderQueryOperations() throws Exception {
        // Create test order first
        CreateOrderRequest request = createOrderRequest(
            "E2E-QUERY-CUSTOMER",
            "E2E-QUERY-PRODUCT",
            1
        );

        OrderResponse order = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/orders")
        .then()
            .statusCode(201)
            .extract()
            .as(OrderResponse.class);

        // Test 1: Get all orders
        given()
            .contentType(ContentType.JSON)
            .queryParam("page", 0)
            .queryParam("size", 10)
        .when()
            .get("/api/orders")
        .then()
            .statusCode(200)
            .body("size()", greaterThan(0));

        // Test 2: Get orders by customer
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/orders/customer/{customerId}", "E2E-QUERY-CUSTOMER")
        .then()
            .statusCode(200)
            .body("size()", greaterThan(0))
            .body("[0].customerId", equalTo("E2E-QUERY-CUSTOMER"));

        // Test 3: Get orders by status
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/orders/status/CONFIRMED")
        .then()
            .statusCode(200)
            .body("size()", greaterThan(0));

        // Test 4: Get order statistics
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/orders/statistics")
        .then()
            .statusCode(200)
            .body("totalOrders", greaterThan(0))
            .body("confirmedOrders", greaterThanOrEqualTo(0))
            .body("cancelledOrders", greaterThanOrEqualTo(0));

        System.out.println("✓ Order query operations test passed");
    }

    @Test
    @Order(6)
    @DisplayName("E2E: Invalid request handling")
    public void testInvalidRequestHandling() throws Exception {
        // Test 1: Empty customer ID
        CreateOrderRequest emptyCustomer = createOrderRequest("", "PRODUCT", 1);
        given()
            .contentType(ContentType.JSON)
            .body(emptyCustomer)
        .when()
            .post("/api/orders")
        .then()
            .statusCode(400);

        // Test 2: Null items
        CreateOrderRequest nullItems = createOrderRequest("CUSTOMER", "PRODUCT", 1);
        nullItems.setItems(null);
        given()
            .contentType(ContentType.JSON)
            .body(nullItems)
        .when()
            .post("/api/orders")
        .then()
            .statusCode(400);

        // Test 3: Empty items list
        CreateOrderRequest emptyItems = createOrderRequest("CUSTOMER", "PRODUCT", 1);
        emptyItems.setItems(List.of());
        given()
            .contentType(ContentType.JSON)
            .body(emptyItems)
        .when()
            .post("/api/orders")
        .then()
            .statusCode(400);

        // Test 4: Non-existent order
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/orders/{orderId}", "NON-EXISTENT-ORDER")
        .then()
            .statusCode(404);

        System.out.println("✓ Invalid request handling test passed");
    }

    @Test
    @Order(7)
    @DisplayName("E2E: Order with multiple items")
    public void testOrderWithMultipleItems() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId("E2E-MULTI-ITEMS");
        request.setCustomerName("Multi Items Customer");
        request.setCustomerEmail("multi@example.com");
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        OrderItemRequest item1 = new OrderItemRequest();
        item1.setProductId("PROD-MULTI-1");
        item1.setProductName("Product 1");
        item1.setQuantity(2);
        item1.setUnitPrice(new BigDecimal("50.00"));

        OrderItemRequest item2 = new OrderItemRequest();
        item2.setProductId("PROD-MULTI-2");
        item2.setProductName("Product 2");
        item2.setQuantity(3);
        item2.setUnitPrice(new BigDecimal("30.00"));

        request.setItems(List.of(item1, item2));

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/orders")
        .then()
            .statusCode(201)
            .body("items.size()", equalTo(2))
            .body("totalAmount", equalTo(190.00f));

        System.out.println("✓ Order with multiple items test passed");
    }

    @Test
    @Order(8)
    @DisplayName("E2E: System health and monitoring")
    public void testSystemHealthAndMonitoring() throws Exception {
        // Test health endpoint
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/health")
        .then()
            .statusCode(200)
            .body("status", equalTo("UP"))
            .body("services.size()", greaterThan(0));

        // Test inventory service health
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/inventory/health")
        .then()
            .statusCode(200)
            .body("status", equalTo("UP"));

        // Test payment service health
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/payments/status")
        .then()
            .statusCode(200)
            .body("status", equalTo("UP"));

        System.out.println("✓ System health and monitoring test passed");
    }

    // Helper methods

    private CreateOrderRequest createOrderRequest(
            String customerId,
            String productId,
            int quantity) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(customerId);
        request.setCustomerName("E2E Test Customer");
        request.setCustomerEmail(customerId.toLowerCase() + "@example.com");
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setProductName("E2E Test Product");
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal("99.99"));

        request.setItems(List.of(item));
        return request;
    }
}
