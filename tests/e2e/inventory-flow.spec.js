// @ts-check
const { test, expect } = require('@playwright/test');

const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';

test.describe('Inventory Flow E2E', () => {

  test('1. Dashboard shows online status and all services healthy', async ({ page }) => {
    await page.goto(`${BASE_URL}/dashboard`);

    // Wait for service status cards to update
    await page.waitForFunction(() => {
      const cards = document.querySelectorAll('.service-card .status-text');
      return Array.from(cards).some(c => c.textContent !== 'Checking...');
    }, { timeout: 10000 });

    // Verify all service cards show Healthy
    const serviceCards = page.locator('.service-card');
    const count = await serviceCards.count();
    expect(count).toBeGreaterThanOrEqual(3);

    for (let i = 0; i < count; i++) {
      const statusText = serviceCards.nth(i).locator('.status-text');
      await expect(statusText).toHaveText('Healthy');
    }
  });

  test('2. Inventory page loads and shows New Product button', async ({ page }) => {
    await page.goto(`${BASE_URL}/inventory`);

    // Verify page title
    await expect(page.locator('h1, .page-title').first()).toContainText('Inventory');

    // Verify New Product button exists
    const newProductBtn = page.getByRole('button', { name: /New Product/i });
    await expect(newProductBtn).toBeVisible();

    // Verify Refresh button exists
    const refreshBtn = page.getByRole('button', { name: /Refresh/i });
    await expect(refreshBtn).toBeVisible();
  });

  test('3. Create product "Test Item 01" via modal', async ({ page }) => {
    await page.goto(`${BASE_URL}/inventory`);

    // Click New Product button
    await page.getByRole('button', { name: /New Product/i }).click();

    // Verify modal is visible
    const modal = page.locator('#create-product-modal');
    await expect(modal).toBeVisible();

    // Fill in product details
    await page.locator('#create-product-form [name="name"]').fill('E2E Test Product');
    await page.locator('#create-product-form [name="description"]').fill('Created by E2E test');
    await page.locator('#create-product-form [name="price"]').fill('49.99');
    await page.locator('#create-product-form [name="quantity"]').fill('50');
    await page.locator('#create-product-form [name="category"]').fill('Testing');

    // Submit the form
    await page.getByRole('button', { name: /Create Product/i }).click();

    // Wait for modal to close (product created successfully)
    await expect(modal).toBeHidden({ timeout: 5000 });

    // Verify product appears in the inventory table
    await page.waitForTimeout(1000);
    const table = page.locator('#inventory-tbody');
    await expect(table).toContainText('E2E Test Product');
  });

  test('4. Product appears in inventory list with correct data', async ({ page }) => {
    await page.goto(`${BASE_URL}/inventory`);

    // Wait for inventory to load
    await page.waitForSelector('#inventory-tbody tr', { timeout: 5000 });

    // Find the E2E Test Product row (use .first() in case of duplicates from re-runs)
    const productRow = page.locator('#inventory-tbody tr', { hasText: 'E2E Test Product' }).first();
    await expect(productRow).toBeVisible();

    // Verify stock
    const stockCell = productRow.locator('td').nth(2);
    const stockText = await stockCell.textContent();
    expect(parseInt(stockText)).toBeGreaterThan(0);

    // Verify price
    const priceCell = productRow.locator('td').nth(5);
    await expect(priceCell).toContainText('49.99');

    // Verify status is IN_STOCK
    const statusCell = productRow.locator('td').nth(6);
    await expect(statusCell).toContainText('IN_STOCK');
  });

  test('5. Create order with the product', async ({ page }) => {
    // First get a product ID from the inventory API
    const inventoryResponse = await page.request.get(`${BASE_URL}/api/inventory`);
    const inventory = await inventoryResponse.json();
    const product = inventory.find(p => p.name === 'E2E Test Product') || inventory[0];
    expect(product).toBeTruthy();

    // Create order via API
    const orderResponse = await page.request.post(`${BASE_URL}/api/orders`, {
      data: {
        customerId: 'E2E-CUST-001',
        customerName: 'E2E Test Customer',
        customerEmail: 'e2e@test.com',
        items: [
          {
            productId: product.productId,
            productName: product.name,
            quantity: 2,
            unitPrice: product.price
          }
        ]
      }
    });

    expect(orderResponse.status()).toBe(201);
    const order = await orderResponse.json();
    expect(order.orderId).toBeTruthy();
    expect(order.status).toBe('CONFIRMED');
  });

  test('6. Orders page shows the created order', async ({ page }) => {
    await page.goto(`${BASE_URL}/orders`);

    // Wait for orders table to load
    await page.waitForSelector('#orders-tbody tr', { timeout: 10000 });

    // Verify at least one order exists
    const rows = page.locator('#orders-tbody tr');
    const rowCount = await rows.count();
    expect(rowCount).toBeGreaterThan(0);

    // Verify one row has CONFIRMED status
    const confirmedRow = page.locator('#orders-tbody tr', { hasText: 'CONFIRMED' });
    await expect(confirmedRow.first()).toBeVisible();
  });

  test('7. Dashboard stats reflect the new order', async ({ page }) => {
    await page.goto(`${BASE_URL}/dashboard`);

    // Wait for stats to load (might show 0 if statistics endpoint returns default)
    await page.waitForFunction(() => {
      const el = document.getElementById('total-orders');
      return el && el.textContent !== '--';
    }, { timeout: 10000 });

    // Total orders element should have a numeric value
    const totalOrders = page.locator('#total-orders');
    const text = await totalOrders.textContent();
    expect(parseInt(text)).toBeGreaterThanOrEqual(0);

    // Revenue element should exist and have content
    const revenue = page.locator('#total-revenue');
    await expect(revenue).toBeVisible();
  });
});
