/**
 * k6 Load Test: SSE Connection Churn
 * Tests the resilience of SSE endpoints under connection churn.
 * 
 * Usage:
 *   k6 run --env BASE_URL=http://localhost:8084 --env TOKEN=<jwt> sse-churn.js
 * 
 * Tests:
 *   - Multiple concurrent SSE connections
 *   - Rapid connect/disconnect cycles
 *   - Long-lived connections stability
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// Custom metrics
const sseConnections = new Counter('sse_connections');
const sseDisconnects = new Counter('sse_disconnects');
const sseMessages = new Counter('sse_messages');
const connectionDuration = new Trend('connection_duration', true);
const connectionSuccess = new Rate('connection_success');

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8084';
const TOKEN = __ENV.TOKEN || 'eyJ...';
const TENANT_ID = __ENV.TENANT_ID || 'tenant-001';

export const options = {
    scenarios: {
        // Rapid churn: quick connect/disconnect
        rapid_churn: {
            executor: 'constant-vus',
            vus: 20,
            duration: '2m',
            exec: 'rapidChurn',
            tags: { scenario: 'rapid_churn' },
        },
        // Sustained connections: long-lived SSE
        sustained: {
            executor: 'constant-vus',
            vus: 50,
            duration: '5m',
            exec: 'sustainedConnection',
            tags: { scenario: 'sustained' },
            startTime: '2m30s',
        },
        // Peak connections: simulate traffic spike
        peak: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 100 },
                { duration: '1m', target: 100 },
                { duration: '30s', target: 0 },
            ],
            exec: 'rapidChurn',
            tags: { scenario: 'peak' },
            startTime: '8m',
        },
    },

    thresholds: {
        connection_success: ['rate>0.90'],
        http_req_duration: ['p(95)<2000'],
    },
};

// Rapid connect/disconnect
export function rapidChurn() {
    const shipmentId = `ship-${randomIntBetween(1, 100)}`;

    // Connect to SSE endpoint
    const startTime = Date.now();

    const response = http.get(
        `${BASE_URL}/api/v1/shipments/${shipmentId}/stream`,
        {
            headers: {
                'Authorization': `Bearer ${TOKEN}`,
                'Accept': 'text/event-stream',
            },
            timeout: '5s',
            tags: { endpoint: 'sse' },
        }
    );

    sseConnections.add(1);

    const success = check(response, {
        'SSE connection established': (r) => r.status === 200,
        'Content-Type is event-stream': (r) =>
            r.headers['Content-Type']?.includes('text/event-stream'),
    });

    connectionSuccess.add(success);

    // Short connection duration for churn test
    sleep(randomIntBetween(500, 2000) / 1000);

    const duration = Date.now() - startTime;
    connectionDuration.add(duration);
    sseDisconnects.add(1);
}

// Long-lived sustained connection
export function sustainedConnection() {
    const shipmentId = `ship-${randomIntBetween(1, 50)}`;

    const startTime = Date.now();

    const response = http.get(
        `${BASE_URL}/api/v1/shipments/${shipmentId}/stream`,
        {
            headers: {
                'Authorization': `Bearer ${TOKEN}`,
                'Accept': 'text/event-stream',
            },
            timeout: '60s', // Long timeout for sustained test
            tags: { endpoint: 'sse-sustained' },
        }
    );

    sseConnections.add(1);

    const success = check(response, {
        'SSE connection established': (r) => r.status === 200,
    });

    connectionSuccess.add(success);

    if (response.body) {
        // Count SSE messages (lines starting with 'data:')
        const messages = (response.body.match(/^data:/gm) || []).length;
        sseMessages.add(messages);
    }

    // Hold connection for longer duration
    sleep(randomIntBetween(10, 30));

    const duration = Date.now() - startTime;
    connectionDuration.add(duration);
    sseDisconnects.add(1);
}

export function setup() {
    console.log(`SSE Churn test against ${BASE_URL}`);

    // Verify SSE endpoint exists
    const response = http.get(`${BASE_URL}/actuator/health`, {
        timeout: '5s',
    });

    if (response.status !== 200) {
        console.warn('Health check returned non-200');
    }

    return { startTime: new Date().toISOString() };
}

export function teardown(data) {
    console.log(`SSE Churn test completed. Started: ${data.startTime}`);
}
