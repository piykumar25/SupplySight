/**
 * k6 Load Test: Frontend API Endpoints
 * Tests the main API endpoints used by the frontend application.
 * 
 * Usage:
 *   k6 run --env BASE_URL=http://localhost:8080 --env TOKEN=<jwt> frontend-api.js
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// Custom metrics
const apiSuccess = new Rate('api_success');
const loginLatency = new Trend('login_latency', true);
const shipmentsLatency = new Trend('shipments_list_latency', true);
const shipmentDetailLatency = new Trend('shipment_detail_latency', true);
const alertsLatency = new Trend('alerts_list_latency', true);

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Test users (should match seeded data)
const USERS = [
    { email: 'admin@demo.com', password: 'admin123' },
    { email: 'user@demo.com', password: 'user123' },
];

export const options = {
    scenarios: {
        // Typical user journey simulation
        user_journey: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 10 },
                { duration: '2m', target: 30 },
                { duration: '1m', target: 50 },
                { duration: '2m', target: 50 },
                { duration: '30s', target: 0 },
            ],
            gracefulStop: '10s',
            exec: 'userJourney',
        },
    },

    thresholds: {
        http_req_duration: ['p(95)<1000', 'p(99)<2000'],
        api_success: ['rate>0.95'],
        login_latency: ['p(95)<500'],
        shipments_list_latency: ['p(95)<1000'],
        alerts_list_latency: ['p(95)<500'],
    },
};

// Simulate a complete user journey
export function userJourney() {
    const user = USERS[randomIntBetween(0, USERS.length - 1)];
    let accessToken = '';

    group('1. Login', () => {
        const loginStart = Date.now();
        const response = http.post(
            `${BASE_URL}/api/v1/login`,
            JSON.stringify(user),
            {
                headers: { 'Content-Type': 'application/json' },
                tags: { endpoint: 'login' },
            }
        );

        loginLatency.add(Date.now() - loginStart);

        const success = check(response, {
            'login successful': (r) => r.status === 200,
            'has access token': (r) => {
                try {
                    const body = JSON.parse(r.body);
                    accessToken = body.data?.accessToken || body.accessToken;
                    return !!accessToken;
                } catch {
                    return false;
                }
            },
        });

        apiSuccess.add(success);
    });

    if (!accessToken) {
        return; // Can't continue without token
    }

    const headers = {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
    };

    sleep(randomIntBetween(1, 3));

    group('2. List Shipments', () => {
        const listStart = Date.now();
        const response = http.get(
            `${BASE_URL}/api/v1/shipments?page=0&size=20`,
            { headers, tags: { endpoint: 'shipments-list' } }
        );

        shipmentsLatency.add(Date.now() - listStart);

        const success = check(response, {
            'shipments list returned': (r) => r.status === 200,
            'has shipments data': (r) => {
                try {
                    const body = JSON.parse(r.body);
                    return Array.isArray(body.data?.content || body.content || body);
                } catch {
                    return false;
                }
            },
        });

        apiSuccess.add(success);
    });

    sleep(randomIntBetween(1, 2));

    group('3. View Shipment Detail', () => {
        // Get a random shipment ID (simulated)
        const shipmentId = `ship-${randomIntBetween(1, 100)}`;

        const detailStart = Date.now();
        const response = http.get(
            `${BASE_URL}/api/v1/shipments/${shipmentId}`,
            { headers, tags: { endpoint: 'shipment-detail' } }
        );

        shipmentDetailLatency.add(Date.now() - detailStart);

        // 404 is acceptable if shipment doesn't exist
        const success = check(response, {
            'shipment detail returned': (r) => r.status === 200 || r.status === 404,
        });

        apiSuccess.add(success);
    });

    sleep(randomIntBetween(1, 2));

    group('4. List Alerts', () => {
        const alertsStart = Date.now();
        const response = http.get(
            `${BASE_URL}/api/v1/alerts?page=0&size=20`,
            { headers, tags: { endpoint: 'alerts-list' } }
        );

        alertsLatency.add(Date.now() - alertsStart);

        const success = check(response, {
            'alerts list returned': (r) => r.status === 200,
        });

        apiSuccess.add(success);
    });

    sleep(randomIntBetween(2, 5));
}

export function setup() {
    console.log(`Frontend API load test against ${BASE_URL}`);

    // Verify API is reachable
    const health = http.get(`${BASE_URL}/actuator/health`);
    if (health.status !== 200) {
        console.warn('API health check failed');
    }

    return {};
}
