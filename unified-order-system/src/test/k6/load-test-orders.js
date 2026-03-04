/**
 * K6 Load Test - Order Creation
 *
 * Purpose: Test order creation endpoint under load
 * SLA Targets:
 * - P95 response time: < 500ms
 * - P99 response time: < 1000ms
 * - Error rate: < 1%
 * - Throughput: > 100 req/s
 *
 * Run: k6 run --vus 100 --duration 30s load-test-orders.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const orderCreationTime = new Trend('order_creation_time');
const successfulOrders = new Counter('successful_orders');
const failedOrders = new Counter('failed_orders');

// Test configuration
export const options = {
    stages: [
        { duration: '30s', target: 50 },  // Ramp-up to 50 users
        { duration: '1m', target: 100 },  // Stay at 100 users
        { duration: '30s', target: 200 }, // Spike to 200 users
        { duration: '1m', target: 100 },  // Back to 100 users
        { duration: '30s', target: 0 },   // Ramp-down to 0 users
    ],
    thresholds: {
        'http_req_duration': ['p(95)<500', 'p(99)<1000'], // SLA: 95% < 500ms, 99% < 1s
        'errors': ['rate<0.01'],                          // SLA: < 1% errors
        'http_req_duration{staticAsset:yes}': ['p(95)<100'],
        'checks': ['rate>0.95'],                          // 95% of checks pass
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8090';

// Generate random customer ID
function randomCustomerId() {
    return `CUST-${Math.floor(Math.random() * 10000)}`;
}

// Generate random product ID
function randomProductId() {
    return `PROD-${Math.floor(Math.random() * 100) + 1}`;
}

// Create order payload
function createOrderPayload() {
    return JSON.stringify({
        customerId: randomCustomerId(),
        customerName: `Customer ${Math.floor(Math.random() * 1000)}`,
        customerEmail: `customer${Math.floor(Math.random() * 1000)}@example.com`,
        paymentMethod: ['CREDIT_CARD', 'DEBIT_CARD', 'PIX'][Math.floor(Math.random() * 3)],
        items: [
            {
                productId: randomProductId(),
                productName: `Product ${Math.floor(Math.random() * 100)}`,
                quantity: Math.floor(Math.random() * 5) + 1,
                unitPrice: (Math.random() * 1000).toFixed(2)
            }
        ]
    });
}

export default function() {
    const payload = createOrderPayload();
    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
        tags: { name: 'CreateOrder' }
    };

    // Test 1: Create Order
    const createOrderResponse = http.post(
        `${BASE_URL}/api/orders`,
        payload,
        params
    );

    const createOrderSuccess = check(createOrderResponse, {
        'order created successfully': (r) => r.status === 201,
        'response has orderId': (r) => {
            try {
                return JSON.parse(r.body).orderId !== undefined;
            } catch {
                return false;
            }
        },
        'response has paymentId': (r) => {
            try {
                return JSON.parse(r.body).paymentId !== undefined;
            } catch {
                return false;
            }
        },
        'response has reservationId': (r) => {
            try {
                return JSON.parse(r.body).reservationId !== undefined;
            } catch {
                return false;
            }
        },
        'response time < 500ms': (r) => r.timings.duration < 500,
    });

    errorRate.add(!createOrderSuccess);
    orderCreationTime.add(createOrderResponse.timings.duration);

    if (createOrderSuccess) {
        successfulOrders.add(1);

        // Extract orderId for subsequent tests
        try {
            const orderData = JSON.parse(createOrderResponse.body);
            const orderId = orderData.orderId;

            // Test 2: Get Order by ID (after creation)
            sleep(0.1); // Small delay
            const getOrderResponse = http.get(
                `${BASE_URL}/api/orders/${orderId}`,
                { tags: { name: 'GetOrder' } }
            );

            check(getOrderResponse, {
                'order retrieved successfully': (r) => r.status === 200,
                'retrieved order matches created': (r) => {
                    try {
                        return JSON.parse(r.body).orderId === orderId;
                    } catch {
                        return false;
                    }
                },
            });
        } catch (e) {
            console.error('Error parsing order response:', e);
        }
    } else {
        failedOrders.add(1);
    }

    sleep(1); // Think time between iterations
}

// Test lifecycle hooks
export function setup() {
    console.log(`Starting load test against: ${BASE_URL}`);

    // Health check before test
    const healthResponse = http.get(`${BASE_URL}/api/health`);
    if (healthResponse.status !== 200) {
        console.error('Health check failed! Service may be down.');
        return { healthy: false };
    }

    return { healthy: true };
}

export function teardown(data) {
    if (data.healthy) {
        console.log('Load test completed successfully');
    } else {
        console.log('Load test completed with warnings');
    }
}

export function handleSummary(data) {
    return {
        'load-test-orders-summary.json': JSON.stringify(data, null, 2),
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}

function textSummary(data, options) {
    const indent = options.indent || '';
    const enableColors = options.enableColors || false;

    let summary = '\n' + indent + '=== Load Test Summary ===\n\n';

    // Add test duration
    summary += indent + `Duration: ${data.state.testRunDurationMs / 1000}s\n`;

    // Add metrics
    if (data.metrics.http_req_duration) {
        const p95 = data.metrics.http_req_duration.values['p(95)'];
        const p99 = data.metrics.http_req_duration.values['p(99)'];
        summary += indent + `P95 Response Time: ${p95.toFixed(2)}ms\n`;
        summary += indent + `P99 Response Time: ${p99.toFixed(2)}ms\n`;
    }

    if (data.metrics.errors) {
        const errorRate = (data.metrics.errors.values.rate * 100).toFixed(2);
        summary += indent + `Error Rate: ${errorRate}%\n`;
    }

    if (data.metrics.http_reqs) {
        const totalRequests = data.metrics.http_reqs.values.count;
        const reqPerSec = data.metrics.http_reqs.values.rate;
        summary += indent + `Total Requests: ${totalRequests}\n`;
        summary += indent + `Requests/sec: ${reqPerSec.toFixed(2)}\n`;
    }

    summary += '\n';
    return summary;
}
