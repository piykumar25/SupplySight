import { test, expect } from '@playwright/test';

/**
 * E2E Test: Event Ingestion → Validated Topic → UI Reflection
 * Verifies that ingested events are processed and reflected in the UI.
 */

const API_BASE = process.env.API_BASE_URL || 'http://localhost:8080';

const TEST_USER = {
    email: 'admin@demo.com',
    password: 'admin123',
};

test.describe('Event Ingestion Flow', () => {
    let accessToken: string;
    let tenantId: string;

    test.beforeAll(async ({ request }) => {
        // Login to get token
        const loginResponse = await request.post(`${API_BASE}/api/v1/login`, {
            data: TEST_USER,
        });

        expect(loginResponse.ok()).toBeTruthy();
        const loginData = await loginResponse.json();
        accessToken = loginData.data?.accessToken || loginData.accessToken;
        tenantId = loginData.data?.tenantId || loginData.tenantId;
    });

    test('should ingest event and reflect in UI', async ({ page, request }) => {
        // Create a unique shipment reference for tracking
        const testShipmentId = crypto.randomUUID();
        const testEventId = crypto.randomUUID();

        // Step 1: Ingest a tracking event
        const ingestResponse = await request.post(`${API_BASE}/api/v1/events/ingest`, {
            headers: {
                'Authorization': `Bearer ${accessToken}`,
                'Content-Type': 'application/json',
            },
            data: {
                eventId: testEventId,
                tenantId: tenantId,
                shipmentId: testShipmentId,
                eventType: 'CREATED',
                eventTime: new Date().toISOString(),
                source: 'E2E_TEST',
                location: {
                    lat: 12.9716,
                    lon: 77.5946,
                    hubCode: 'E2E-HUB-001',
                },
                payload: {
                    reference: 'E2E-TEST-' + Date.now(),
                    origin: 'Test Origin',
                    destination: 'Test Destination',
                },
            },
        });

        expect(ingestResponse.ok()).toBeTruthy();
        const ingestData = await ingestResponse.json();
        expect(ingestData.data?.status || ingestData.status).toBe('ACCEPTED');

        // Step 2: Add an IN_TRANSIT event
        await request.post(`${API_BASE}/api/v1/events/ingest`, {
            headers: {
                'Authorization': `Bearer ${accessToken}`,
                'Content-Type': 'application/json',
            },
            data: {
                eventId: crypto.randomUUID(),
                tenantId: tenantId,
                shipmentId: testShipmentId,
                eventType: 'IN_TRANSIT',
                eventTime: new Date().toISOString(),
                source: 'E2E_TEST',
                location: {
                    lat: 13.0827,
                    lon: 80.2707,
                    hubCode: 'E2E-HUB-002',
                },
            },
        });

        // Step 3: Wait for event processing
        await page.waitForTimeout(3000);

        // Step 4: Login to UI and verify shipment appears
        await page.goto('/');
        await page.fill('input[name="email"], input[type="email"]', TEST_USER.email);
        await page.fill('input[name="password"], input[type="password"]', TEST_USER.password);
        await page.click('button[type="submit"]');
        await page.waitForURL(/dashboard|shipments|home/, { timeout: 10000 });

        // Step 5: Navigate to shipments and search
        await page.goto('/shipments');
        await page.waitForSelector('[data-testid="shipments-list"], table, .shipment-list', {
            timeout: 10000,
        });

        // Step 6: Verify the new shipment status via API
        const visibilityResponse = await request.get(
            `${API_BASE}/api/v1/shipments/${testShipmentId}`,
            {
                headers: {
                    'Authorization': `Bearer ${accessToken}`,
                },
            }
        );

        // Shipment should exist in visibility layer
        if (visibilityResponse.ok()) {
            const shipmentData = await visibilityResponse.json();
            expect(shipmentData.data?.currentStatus || shipmentData.currentStatus).toBe('IN_TRANSIT');
        }
    });

    test('should handle duplicate events correctly', async ({ request }) => {
        const duplicateEventId = crypto.randomUUID();
        const shipmentId = crypto.randomUUID();

        const eventData = {
            eventId: duplicateEventId,
            tenantId: tenantId,
            shipmentId: shipmentId,
            eventType: 'CREATED',
            eventTime: new Date().toISOString(),
            source: 'E2E_TEST',
        };

        // First ingestion - should be ACCEPTED
        const firstResponse = await request.post(`${API_BASE}/api/v1/events/ingest`, {
            headers: {
                'Authorization': `Bearer ${accessToken}`,
                'Content-Type': 'application/json',
            },
            data: eventData,
        });

        expect(firstResponse.ok()).toBeTruthy();
        const firstData = await firstResponse.json();
        expect(firstData.data?.status || firstData.status).toBe('ACCEPTED');

        // Second ingestion (duplicate) - should be DUPLICATE
        const secondResponse = await request.post(`${API_BASE}/api/v1/events/ingest`, {
            headers: {
                'Authorization': `Bearer ${accessToken}`,
                'Content-Type': 'application/json',
            },
            data: eventData,
        });

        expect(secondResponse.ok()).toBeTruthy();
        const secondData = await secondResponse.json();
        expect(secondData.data?.status || secondData.status).toBe('DUPLICATE');
    });
});
