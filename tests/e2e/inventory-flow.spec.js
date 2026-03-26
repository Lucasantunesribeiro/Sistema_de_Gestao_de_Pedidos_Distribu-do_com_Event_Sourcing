// @ts-check
const { test, expect } = require('@playwright/test');

const API_BASE_URL = process.env.API_BASE_URL || 'http://localhost:8080';
const ADMIN_USERNAME = process.env.E2E_USERNAME || 'admin';
const ADMIN_PASSWORD = process.env.E2E_PASSWORD || 'change-this-admin-password';

async function loginViaUi(page) {
  await page.goto('/login');
  await page.waitForLoadState('networkidle');
  console.log('[e2e] after goto /login — url:', page.url());
  await page.getByLabel('Username or email').fill(ADMIN_USERNAME);
  await page.getByLabel('Password').fill(ADMIN_PASSWORD);
  await page.getByRole('button', { name: /sign in/i }).click();
  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.getByRole('button', { name: /logout/i })).toBeVisible();
}

async function fetchAccessToken(request) {
  const response = await request.post(`${API_BASE_URL}/api/auth/login`, {
    data: {
      username: ADMIN_USERNAME,
      password: ADMIN_PASSWORD,
    },
  });

  expect(response.ok()).toBeTruthy();
  const body = await response.json();
  return body.accessToken;
}

async function seedProduct(request, suffix) {
  const accessToken = await fetchAccessToken(request);
  const response = await request.post(`${API_BASE_URL}/api/inventory/products`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
    data: {
      name: `E2E Product ${suffix}`,
      description: 'Playwright seeded product',
      price: 49.99,
      quantity: 25,
      category: 'E2E',
    },
  });

  expect(response.status()).toBe(201);
  return response.json();
}

test.describe('Inventory Flow E2E', () => {
  test.beforeEach(async ({ page }) => {
    page.on('pageerror', err => console.error('[page error]', err.message));
    page.on('console', msg => {
      if (msg.type() === 'error') console.error('[browser]', msg.text());
    });
  });

  test('1. Login redirects into the Angular dashboard', async ({ page }) => {
    await page.goto('/login');
    await page.waitForLoadState('networkidle');
    console.log('[e2e] test1 after goto /login — url:', page.url());
    await expect(page.getByRole('heading', { name: /orderflow control plane/i })).toBeVisible();

    await loginViaUi(page);

    await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible();
    await expect(page.getByText('Order Management System')).toBeVisible();
  });

  test('2. Inventory page renders seeded products through the Angular API client', async ({ page, request }) => {
    const product = await seedProduct(request, Date.now());

    await loginViaUi(page);
    await page.goto('/inventory');

    await expect(page.getByRole('heading', { name: /inventory/i })).toBeVisible();
    await expect(page.getByText(product.name)).toBeVisible();
    await expect(page.getByText(product.productId)).toBeVisible();
    await expect(page.getByText('Total Products')).toBeVisible();
    await expect(page.getByRole('table', { name: /inventory items/i })).toContainText(product.name);
  });

  test('3. Create order flow runs through the Angular SPA', async ({ page, request }) => {
    const product = await seedProduct(request, `order-${Date.now()}`);
    const customerId = `CUST-${Date.now()}`;
    const customerName = `E2E Customer ${Date.now()}`;

    await loginViaUi(page);
    await page.goto('/orders/new');

    await page.getByLabel(/customer id/i).fill(customerId);
    await page.getByLabel(/customer name/i).fill(customerName);
    await page.locator('#paymentMethod').selectOption('PIX');
    await page.locator('#productId-0').fill(product.productId);
    await page.locator('#productName-0').fill(product.name);
    await page.locator('#quantity-0').fill('2');
    await page.locator('#unitPrice-0').fill('49.99');
    await page.getByRole('button', { name: /create order/i }).click();

    await expect(page).toHaveURL(/\/orders\/.+/);
    await expect(page.getByText(customerName)).toBeVisible();
    await expect(page.getByText(product.name)).toBeVisible();
    await expect(page.getByText('PIX')).toBeVisible();

    await page.goto('/orders');
    await expect(page.getByRole('table', { name: /orders table/i })).toContainText(customerName);
  });
});
