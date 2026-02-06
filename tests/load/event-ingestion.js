/**
 * k6 Load Test: Event Ingestion
 * Tests the throughput and latency of the event ingestion endpoint.
 * 
 * Usage:
 *   k6 run --env BASE_URL=http://localhost:8082 --env TOKEN=<jwt> event-ingestion.js
 * 
 * Scenarios:
 *   - smoke: 1 VU for 30s (baseline check)
 *   - load: Ramp to 50 VUs over 2m, sustain for 5m
 *   - stress: Ramp to 100 VUs, check breaking point
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// Custom metrics
const eventsSent = new Counter('events_sent');
const eventsAccepted = new Counter('events_accepted');
const eventsDuplicate = new Counter('events_duplicate');
const eventsRejected = new Counter('events_rejected');
const ingestionLatency = new Trend('ingestion_latency', true);
const successRate = new Rate('success_rate');

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8082';
const TOKEN = __ENV.TOKEN || 'eyJ...'; // JWT token for auth
const TENANT_ID = __ENV.TENANT_ID || 'tenant-001';

// Test scenarios
export const options = {
    scenarios: {
        // Smoke test: verify endpoint works
        smoke: {
            executor: 'constant-vus',
            vus: 1,
            duration: '30s',
            gracefulStop: '5s',
            exec: 'ingestEvents',
            tags: { scenario: 'smoke' },
        },
        // Load test: normal expected load
        load: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '1m', target: 20 },  // Ramp up
                { duration: '3m', target: 20 },  // Sustain
                { duration: '1m', target: 50 },  // Push higher
                { duration: '2m', target: 50 },  // Sustain
                { duration: '1m', target: 0 },   // Ramp down
            ],
            gracefulStop: '10s',
            exec: 'ingestEvents',
            tags: { scenario: 'load' },
            startTime: '35s', // Start after smoke test
        },
        // Stress test: find breaking point
        stress: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '2m', target: 50 },
                { duration: '3m', target: 100 },
                { duration: '2m', target: 150 },
                { duration: '1m', target: 0 },
            ],
            gracefulStop: '10s',
            exec: 'ingestEvents',
            tags: { scenario: 'stress' },
            startTime: '10m', // Start after load test
        },
    },

    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1000'],
        success_rate: ['rate>0.95'],
        events_accepted: ['count>0'],
    },
};

// Event types to simulate
const EVENT_TYPES = [
    'CREATED',
    'PICKED_UP',
    'IN_TRANSIT',
    'OUT_FOR_DELIVERY',
    'ARRIVED_AT_HUB',
    'DEPARTED_HUB',
    'CUSTOMS_HOLD',
    'DELIVERED',
];

// Generate random event
function generateEvent(vuId, iteration) {
    const eventId = `k6-${vuId}-${iteration}-${Date.now()}`;
    const shipmentId = `ship-k6-${vuId % 100}`; // Reuse shipment IDs to simulate real flow

    return {
        eventId: eventId,
        tenantId: TENANT_ID,
        shipmentId: shipmentId,
        eventType: EVENT_TYPES[randomIntBetween(0, EVENT_TYPES.length - 1)],
        eventTime: new Date().toISOString(),
        source: 'k6-load-test',
        location: {
            lat: 12.9716 + (Math.random() * 2 - 1),
            lon: 77.5946 + (Math.random() * 2 - 1),
            hubCode: `HUB-${randomIntBetween(1, 10)}`,
        },
        payload: {
            testRun: __ENV.K6_TEST_RUN_ID || 'local',
            vuId: vuId,
            iteration: iteration,
        },
    };
}

// Main test function
export function ingestEvents() {
    const event = generateEvent(__VU, __ITER);

    const response = http.post(
        `${BASE_URL}/api/v1/events/ingest`,
        JSON.stringify(event),
        {
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${TOKEN}`,
                'X-Correlation-Id': `k6-${__VU}-${__ITER}`,
            },
            tags: { endpoint: 'ingest' },
        }
    );

    eventsSent.add(1);
    ingestionLatency.add(response.timings.duration);

    const success = check(response, {
        'status is 200/202': (r) => r.status === 200 || r.status === 202,
        'response has status field': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.status || body.data?.status;
            } catch {
                return false;
            }
        },
    });

    successRate.add(success);

    if (response.status === 200 || response.status === 202) {
        try {
            const body = JSON.parse(response.body);
            const status = body.status || body.data?.status;

            if (status === 'ACCEPTED') {
                eventsAccepted.add(1);
            } else if (status === 'DUPLICATE') {
                eventsDuplicate.add(1);
            } else {
                eventsRejected.add(1);
            }
        } catch {
            eventsRejected.add(1);
        }
    } else {
        eventsRejected.add(1);
    }

    // Short pause between requests
    sleep(randomIntBetween(100, 300) / 1000);
}

// Setup - runs once at start
export function setup() {
    console.log(`Starting load test against ${BASE_URL}`);
    console.log(`Tenant: ${TENANT_ID}`);

    // Verify endpoint is reachable
    const healthCheck = http.get(`${BASE_URL}/actuator/health`, {
        timeout: '5s',
    });

    if (healthCheck.status !== 200) {
        console.error(`Health check failed: ${healthCheck.status}`);
    }

    return { startTime: new Date().toISOString() };
}

// Teardown - runs once at end
export function teardown(data) {
    console.log(`Load test completed. Started at: ${data.startTime}`);
}
