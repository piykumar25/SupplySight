import { test, expect, Page } from '@playwright/test';

/**
 * E2E Test: Login → Search Shipments → Open Detail → Alert Acknowledgment
 * Critical flow for verifying the main user journey works end-to-end.
 */

const API_BASE = process.env.API_BASE_URL || 'http://localhost:8080';

// Test credentials
const TEST_USER = {
    email: 'admin@demo.com',
    password: 'admin123',
};

test.describe('Alert Acknowledgment Flow', () => {
    let accessToken: string;
    let tenantId: string;

    test.beforeAll(async ({ request }) => {
        // Login via API to get token
        const loginResponse = await request.post(`${API_BASE}/api/v1/login`, {
            data: TEST_USER,
        });

        expect(loginResponse.ok()).toBeTruthy();
        const loginData = await loginResponse.json();
        accessToken = loginData.data?.accessToken || loginData.accessToken;
        tenantId = loginData.data?.tenantId || loginData.tenantId;
    });

    test('should login and view dashboard', async ({ page }) => {
        await page.goto('/');

        // Should redirect to login
        await expect(page).toHaveURL(/login/);

        // Fill login form
        await page.fill('input[name="email"], input[type="email"]', TEST_USER.email);
        await page.fill('input[name="password"], input[type="password"]', TEST_USER.password);
        await page.click('button[type="submit"]');

        // Should redirect to dashboard after login
        await page.waitForURL(/dashboard|shipments|home/, { timeout: 10000 });

        // Verify dashboard loaded
        await expect(page.locator('h1, [data-testid="dashboard-title"]')).toBeVisible();
    });

    test('should search and view shipment details', async ({ page }) => {
        // Login first
        await loginViaUI(page);

        // Navigate to shipments
        await page.click('a[href*="shipments"], [data-testid="shipments-link"]');
        await page.waitForURL(/shipments/);

        // Wait for shipments list to load
        await page.waitForSelector('[data-testid="shipment-row"], .shipment-card, table tbody tr', {
            timeout: 10000,
        });

        // Click on first shipment
        const firstShipment = page.locator('[data-testid="shipment-row"], .shipment-card, table tbody tr').first();
        await firstShipment.click();

        // Verify detail page loaded
        await page.waitForURL(/shipments\/[a-f0-9-]+/);
        await expect(page.locator('[data-testid="shipment-detail"], .shipment-detail')).toBeVisible();
    });

    test('should trigger and acknowledge alert', async ({ page, request }) => {
        // Login first
        await loginViaUI(page);

        // Navigate to shipments
        await page.goto('/shipments');
        await page.waitForSelector('[data-testid="shipment-row"], .shipment-card, table tbody tr');

        // Get first shipment ID from URL or data attribute
        const firstShipment = page.locator('[data-testid="shipment-row"], table tbody tr').first();
        const shipmentLink = await firstShipment.locator('a').first().getAttribute('href');
        const shipmentId = shipmentLink?.match(/shipments\/([a-f0-9-]+)/)?.[1];

        if (shipmentId) {
            // Trigger a DELAY_RISK_HIGH event via API
            const eventResponse = await request.post(`${API_BASE}/api/v1/events/ingest`, {
                headers: {
                    'Authorization': `Bearer ${accessToken}`,
                    'Content-Type': 'application/json',
                },
                data: {
                    eventId: crypto.randomUUID(),
                    tenantId: tenantId,
                    shipmentId: shipmentId,
                    eventType: 'DELAY_RISK_HIGH',
                    eventTime: new Date().toISOString(),
                    source: 'E2E_TEST',
                    payload: {
                        riskLevel: 'HIGH',
                        reason: 'E2E Test Alert',
                    },
                },
            });

            // Allow some time for event processing
            await page.waitForTimeout(2000);
        }

        // Navigate to alerts page
        await page.click('a[href*="alerts"], [data-testid="alerts-link"]');
        await page.waitForURL(/alerts/);

        // Wait for alerts to load
        await page.waitForSelector('[data-testid="alert-row"], .alert-item, table tbody tr', {
            timeout: 15000,
        });

        // Find unacknowledged alert
        const alertRow = page.locator('[data-testid="alert-row"]:not(.acknowledged), .alert-item:not(.acknowledged)').first();

        if (await alertRow.isVisible()) {
            // Click acknowledge button
            const ackButton = alertRow.locator('[data-testid="ack-button"], button:has-text("Acknowledge")');
            await ackButton.click();

            // Verify alert is acknowledged
            await expect(alertRow).toHaveClass(/acknowledged|resolved/);
        }
    });
});

async function loginViaUI(page: Page) {
    await page.goto('/');

    // Check if already logged in
    if (page.url().includes('dashboard') || page.url().includes('shipments')) {
        return;
    }

    await page.fill('input[name="email"], input[type="email"]', TEST_USER.email);
    await page.fill('input[name="password"], input[type="password"]', TEST_USER.password);
    await page.click('button[type="submit"]');
    await page.waitForURL(/dashboard|shipments|home/, { timeout: 10000 });
}
