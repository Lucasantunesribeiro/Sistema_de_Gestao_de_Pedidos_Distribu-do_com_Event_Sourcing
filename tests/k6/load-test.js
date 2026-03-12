/**
 * k6 Load Test — OrderFlow
 *
 * Simulates a realistic order management workload:
 *   - 70% read (GET orders/stats)
 *   - 20% write (create order)
 *   - 10% cancel
 *
 * Usage:
 *   k6 run tests/k6/load-test.js
 *   k6 run --vus 50 --duration 60s tests/k6/load-test.js
 *
 * Environment variables:
 *   BASE_URL  – target base URL (default: http://localhost:8080)
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { randomItem, randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

// ---------------------------------------------------------------------------
// Custom metrics
// ---------------------------------------------------------------------------
const orderCreationRate   = new Rate('order_creation_success');
const orderCancelRate     = new Rate('order_cancel_success');
const orderQueryRate      = new Rate('order_query_success');
const orderCreationTime   = new Trend('order_creation_duration', true);
const ordersCreated       = new Counter('orders_created_total');

// ---------------------------------------------------------------------------
// Test configuration
// ---------------------------------------------------------------------------
export const options = {
  scenarios: {
    smoke: {
      executor: 'constant-vus',
      vus: 5,
      duration: '30s',
      tags: { scenario: 'smoke' },
    },
    load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 20 },
        { duration: '60s', target: 50 },
        { duration: '30s', target: 0 },
      ],
      startTime: '35s',
      tags: { scenario: 'load' },
    },
  },
  thresholds: {
    http_req_duration:         ['p(95)<2000'],   // 95th percentile < 2 s
    http_req_failed:           ['rate<0.05'],    // error rate < 5%
    order_creation_success:    ['rate>0.90'],    // >90% successful creations
    order_creation_duration:   ['p(95)<3000'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// ---------------------------------------------------------------------------
// Sample data
// ---------------------------------------------------------------------------
const CUSTOMERS = ['customer-001', 'customer-002', 'customer-003', 'customer-004'];
const PRODUCTS  = [
  { productId: 'prod-001', productName: 'Widget A', unitPrice: 29.99 },
  { productId: 'prod-002', productName: 'Widget B', unitPrice: 49.99 },
  { productId: 'prod-003', productName: 'Gadget X', unitPrice: 99.99 },
];
const PAYMENT_METHODS = ['PIX', 'CREDIT_CARD', 'DEBIT_CARD'];

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------
const JSON_HEADERS = { 'Content-Type': 'application/json' };

function buildOrderPayload() {
  const product  = randomItem(PRODUCTS);
  const quantity = randomIntBetween(1, 5);
  return JSON.stringify({
    customerId:    randomItem(CUSTOMERS),
    customerName:  'Load Test User',
    paymentMethod: randomItem(PAYMENT_METHODS),
    items: [{
      productId:   product.productId,
      productName: product.productName,
      quantity:    quantity,
      unitPrice:   product.unitPrice,
    }],
  });
}

// ---------------------------------------------------------------------------
// Main virtual-user function
// ---------------------------------------------------------------------------
export default function () {
  const rand = Math.random();

  if (rand < 0.70) {
    // --- Read workload ---
    group('GET /api/orders', () => {
      const res = http.get(`${BASE_URL}/api/orders?page=0&size=10`);
      const ok  = check(res, { 'list orders 200': (r) => r.status === 200 });
      orderQueryRate.add(ok);
    });

  } else if (rand < 0.90) {
    // --- Create order ---
    group('POST /api/orders', () => {
      const start = Date.now();
      const res   = http.post(`${BASE_URL}/api/orders`, buildOrderPayload(), { headers: JSON_HEADERS });
      orderCreationTime.add(Date.now() - start);

      const ok = check(res, {
        'create order 201': (r) => r.status === 201,
        'has orderId':      (r) => {
          try { return JSON.parse(r.body).orderId !== undefined; }
          catch { return false; }
        },
      });
      orderCreationRate.add(ok);
      if (ok) ordersCreated.add(1);

      // Store orderId for potential cancel
      if (ok && res.body) {
        try {
          const body    = JSON.parse(res.body);
          const orderId = body.orderId;

          // 10% chance of immediate cancel
          if (orderId && Math.random() < 0.10) {
            sleep(0.5);
            const cancelRes = http.put(
              `${BASE_URL}/api/orders/${orderId}/cancel`,
              JSON.stringify({ reason: 'Load test cancellation', cancelledBy: 'k6' }),
              { headers: JSON_HEADERS }
            );
            const cancelOk = check(cancelRes, { 'cancel order 200': (r) => r.status === 200 });
            orderCancelRate.add(cancelOk);
          }
        } catch { /* ignore parse errors */ }
      }
    });

  } else {
    // --- Statistics ---
    group('GET /api/orders/statistics', () => {
      const res = http.get(`${BASE_URL}/api/orders/statistics`);
      check(res, { 'statistics 200': (r) => r.status === 200 });
    });
  }

  sleep(randomIntBetween(1, 3));
}

// ---------------------------------------------------------------------------
// Setup: verify the service is reachable before the test
// ---------------------------------------------------------------------------
export function setup() {
  const res = http.get(`${BASE_URL}/actuator/health`);
  if (res.status !== 200) {
    throw new Error(`Service unreachable at ${BASE_URL} (status=${res.status})`);
  }
  console.log(`k6 load test targeting: ${BASE_URL}`);
}
