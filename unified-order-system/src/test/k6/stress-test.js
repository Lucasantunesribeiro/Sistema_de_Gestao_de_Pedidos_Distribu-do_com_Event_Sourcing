/**
 * K6 Stress Test - Breaking Point Analysis
 *
 * Purpose: Find system breaking point and bottlenecks
 * This test gradually increases load until system fails
 *
 * Run: k6 run stress-test.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const errorRate = new Rate('errors');
const responseTime = new Trend('response_time');

export const options = {
    stages: [
        { duration: '2m', target: 100 },   // Normal load
        { duration: '3m', target: 200 },   // High load
        { duration: '3m', target: 500 },   // Very high load
        { duration: '3m', target: 1000 },  // Extreme load
        { duration: '2m', target: 1500 },  // Breaking point
        { duration: '2m', target: 2000 },  // Beyond breaking point
        { duration: '5m', target: 0 },     // Recovery
    ],
    thresholds: {
        'http_req_failed': ['rate<0.10'], // Allow 10% failure during stress
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8090';

export default function() {
    const payload = JSON.stringify({
        customerId: `STRESS-${__VU}-${__ITER}`,
        customerName: `Stress Test User ${__VU}`,
        customerEmail: `stress${__VU}@example.com`,
        paymentMethod: 'CREDIT_CARD',
        items: [{
            productId: `PROD-${Math.floor(Math.random() * 10) + 1}`,
            productName: 'Stress Test Product',
            quantity: 1,
            unitPrice: '99.99'
        }]
    });

    const response = http.post(
        `${BASE_URL}/api/orders`,
        payload,
        { headers: { 'Content-Type': 'application/json' } }
    );

    const success = check(response, {
        'status is 201': (r) => r.status === 201,
        'no server errors': (r) => r.status < 500,
    });

    errorRate.add(!success);
    responseTime.add(response.timings.duration);

    sleep(1);
}

export function handleSummary(data) {
    console.log('\n=== STRESS TEST RESULTS ===\n');
    console.log(`Total Requests: ${data.metrics.http_reqs.values.count}`);
    console.log(`Failed Requests: ${data.metrics.http_req_failed.values.passes}`);
    console.log(`Error Rate: ${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%`);
    console.log(`Avg Response Time: ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms`);
    console.log(`P95 Response Time: ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms`);
    console.log(`P99 Response Time: ${data.metrics.http_req_duration.values['p(99)'].toFixed(2)}ms`);

    return {
        'stress-test-results.json': JSON.stringify(data, null, 2),
    };
}
