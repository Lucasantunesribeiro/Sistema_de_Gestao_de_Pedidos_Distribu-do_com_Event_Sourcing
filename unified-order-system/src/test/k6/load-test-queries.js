/**
 * K6 Load Test - Query Operations
 *
 * Purpose: Test query endpoints under load
 * SLA Targets:
 * - P95 response time: < 200ms
 * - P99 response time: < 500ms
 * - Error rate: < 0.1%
 * - Throughput: > 500 req/s
 *
 * Run: k6 run --vus 200 --duration 60s load-test-queries.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const queryTime = new Trend('query_time');
const successfulQueries = new Counter('successful_queries');

export const options = {
    stages: [
        { duration: '30s', target: 100 },
        { duration: '2m', target: 200 },
        { duration: '30s', target: 500 }, // Spike test
        { duration: '1m', target: 200 },
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        'http_req_duration': ['p(95)<200', 'p(99)<500'],
        'errors': ['rate<0.001'],
        'checks': ['rate>0.98'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8090';

export default function() {
    const tests = [
        // Test 1: Get all orders
        () => {
            const response = http.get(`${BASE_URL}/api/orders?page=0&size=20`, {
                tags: { name: 'GetAllOrders' }
            });
            return check(response, {
                'orders retrieved': (r) => r.status === 200,
                'response is array': (r) => {
                    try {
                        return Array.isArray(JSON.parse(r.body));
                    } catch {
                        return false;
                    }
                },
            });
        },

        // Test 2: Get orders by status
        () => {
            const statuses = ['CONFIRMED', 'PENDING', 'CANCELLED'];
            const status = statuses[Math.floor(Math.random() * statuses.length)];
            const response = http.get(`${BASE_URL}/api/orders/status/${status}`, {
                tags: { name: 'GetOrdersByStatus' }
            });
            return check(response, {
                'orders by status retrieved': (r) => r.status === 200,
            });
        },

        // Test 3: Get order statistics
        () => {
            const response = http.get(`${BASE_URL}/api/orders/statistics`, {
                tags: { name: 'GetOrderStatistics' }
            });
            return check(response, {
                'statistics retrieved': (r) => r.status === 200,
                'has totalOrders field': (r) => {
                    try {
                        return JSON.parse(r.body).totalOrders !== undefined;
                    } catch {
                        return false;
                    }
                },
            });
        },

        // Test 4: Get inventory status
        () => {
            const response = http.get(`${BASE_URL}/api/inventory/status`, {
                tags: { name: 'GetInventoryStatus' }
            });
            return check(response, {
                'inventory status retrieved': (r) => r.status === 200,
            });
        },

        // Test 5: Get all products
        () => {
            const response = http.get(`${BASE_URL}/api/inventory/products`, {
                tags: { name: 'GetAllProducts' }
            });
            return check(response, {
                'products retrieved': (r) => r.status === 200,
                'response is array': (r) => {
                    try {
                        return Array.isArray(JSON.parse(r.body));
                    } catch {
                        return false;
                    }
                },
            });
        },
    ];

    // Execute random test
    const test = tests[Math.floor(Math.random() * tests.length)];
    const startTime = Date.now();
    const success = test();
    const duration = Date.now() - startTime;

    errorRate.add(!success);
    queryTime.add(duration);

    if (success) {
        successfulQueries.add(1);
    }

    sleep(0.5); // Shorter think time for read operations
}

export function setup() {
    console.log(`Starting query load test against: ${BASE_URL}`);
    const healthResponse = http.get(`${BASE_URL}/api/health`);
    return { healthy: healthResponse.status === 200 };
}
