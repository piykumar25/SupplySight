# SupplySight API Specification

This document provides API specifications and sample curl commands for all SupplySight services.

## Table of Contents
1. [Identity Service](#identity-service)
2. [Event Ingestion Service](#event-ingestion-service)
3. [Tracking Service](#tracking-service)
4. [Visibility Projection Service](#visibility-projection-service)
5. [Prediction Engine Service](#prediction-engine-service)

---

## Identity Service

Base URL: `http://localhost:8081/api/v1`

### Authentication

#### Login

Authenticate with email and password to receive JWT tokens.

**Request:**
```bash
curl -X POST http://localhost:8081/api/v1/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@demo.com",
    "password": "admin123"
  }'
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890-...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "tenantId": "123e4567-e89b-12d3-a456-426614174000",
      "tenantCode": "demo-tenant",
      "email": "admin@demo.com",
      "username": "admin",
      "fullName": "Admin User",
      "roles": ["ADMIN"]
    }
  },
  "timestamp": "2026-01-23T10:00:00Z"
}
```

#### Refresh Token

Get a new access token using refresh token.

**Request:**
```bash
curl -X POST http://localhost:8081/api/v1/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890-..."
  }'
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  },
  "timestamp": "2026-01-23T10:00:00Z"
}
```

#### Get Current User

**Request:**
```bash
curl -X GET http://localhost:8081/api/v1/me \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "tenantId": "123e4567-e89b-12d3-a456-426614174000",
    "tenantName": "Demo Company",
    "tenantCode": "demo-tenant",
    "email": "admin@demo.com",
    "username": "admin",
    "firstName": "Admin",
    "lastName": "User",
    "fullName": "Admin User",
    "roles": ["ADMIN"]
  },
  "timestamp": "2026-01-23T10:00:00Z"
}
```

#### Logout

**Request:**
```bash
curl -X POST http://localhost:8081/api/v1/logout \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890-..."
  }'
```

### Tenant Management (ADMIN only)

#### Create Tenant

**Request:**
```bash
curl -X POST http://localhost:8081/api/v1/tenants \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "New Company",
    "code": "new-company",
    "contactEmail": "admin@newcompany.com",
    "contactPhone": "+1-555-0100",
    "address": "123 Business St"
  }'
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "id": "789e0123-e456-7890-abcd-ef1234567890",
    "name": "New Company",
    "code": "new-company",
    "status": "ACTIVE",
    "contactEmail": "admin@newcompany.com",
    "contactPhone": "+1-555-0100",
    "address": "123 Business St",
    "createdAt": "2026-01-23T10:00:00Z"
  },
  "timestamp": "2026-01-23T10:00:00Z"
}
```

#### Get Tenant

**Request:**
```bash
curl -X GET http://localhost:8081/api/v1/tenants/{tenantId} \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

#### List Tenants

**Request:**
```bash
curl -X GET "http://localhost:8081/api/v1/tenants?page=0&size=20&sortBy=createdAt&sortDir=desc" \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

### User Management

#### Create User

**Request:**
```bash
curl -X POST http://localhost:8081/api/v1/users \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@example.com",
    "username": "newuser",
    "password": "securePassword123",
    "firstName": "New",
    "lastName": "User",
    "roles": ["OPS_USER"]
  }'
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "id": "abc12345-e678-9012-abcd-ef3456789012",
    "tenantId": "123e4567-e89b-12d3-a456-426614174000",
    "email": "newuser@example.com",
    "username": "newuser",
    "firstName": "New",
    "lastName": "User",
    "fullName": "New User",
    "roles": ["OPS_USER"],
    "status": "ACTIVE",
    "createdAt": "2026-01-23T10:00:00Z"
  },
  "timestamp": "2026-01-23T10:00:00Z"
}
```

#### List Users

**Request:**
```bash
curl -X GET "http://localhost:8081/api/v1/users?page=0&size=20" \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

---

## Event Ingestion Service

Base URL: `http://localhost:8082/api/v1`

### Ingest Event

**Request:**
```bash
curl -X POST http://localhost:8082/api/v1/events/ingest \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "evt-12345678-abcd-efgh-ijkl-mnopqrstuvwx",
    "tenantId": "123e4567-e89b-12d3-a456-426614174000",
    "shipmentId": "shp-98765432-wxyz-uvst-rqpo-nmlkjihgfedcba",
    "eventType": "IN_TRANSIT",
    "eventTime": "2026-01-23T10:20:30Z",
    "source": "GPS_DEVICE",
    "location": {
      "lat": 12.9716,
      "lon": 77.5946,
      "hubCode": "BLR-HUB-01"
    },
    "payload": {
      "speedKmph": 62,
      "remarks": "moving"
    },
    "metadata": {
      "correlationId": "corr-abcdef12-3456-7890-abcd-ef1234567890"
    }
  }'
```

**Response (202 Accepted):**
```json
{
  "success": true,
  "data": {
    "eventId": "evt-12345678-abcd-efgh-ijkl-mnopqrstuvwx",
    "status": "ACCEPTED",
    "message": "Event accepted for processing"
  },
  "timestamp": "2026-01-23T10:20:35Z"
}
```

---

## Tracking Service

Base URL: `http://localhost:8083/api/v1`

### Create Shipment

**Request:**
```bash
curl -X POST http://localhost:8083/api/v1/shipments \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "trackingNumber": "SHIP-2026-001234",
    "origin": {
      "lat": 12.9716,
      "lon": 77.5946,
      "address": "Warehouse A, Bangalore"
    },
    "destination": {
      "lat": 19.0760,
      "lon": 72.8777,
      "address": "Distribution Center B, Mumbai"
    },
    "expectedDeliveryDate": "2026-01-25T18:00:00Z",
    "cargoDescription": "Electronics",
    "weight": 150.5,
    "weightUnit": "KG"
  }'
```

### Get Shipment

**Request:**
```bash
curl -X GET http://localhost:8083/api/v1/shipments/{shipmentId} \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

---

## Visibility Projection Service

Base URL: `http://localhost:8084/api/v1`

### Get Shipment Current State

**Request:**
```bash
curl -X GET http://localhost:8084/api/v1/shipments/{shipmentId} \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "shipmentId": "shp-98765432-wxyz-uvst-rqpo-nmlkjihgfedcba",
    "tenantId": "123e4567-e89b-12d3-a456-426614174000",
    "status": "IN_TRANSIT",
    "lastEventTime": "2026-01-23T10:20:30Z",
    "lastLocation": {
      "lat": 12.9716,
      "lon": 77.5946,
      "hubCode": "BLR-HUB-01"
    },
    "eta": "2026-01-25T14:30:00Z",
    "delayProbability": 0.15,
    "updatedAt": "2026-01-23T10:20:35Z"
  },
  "timestamp": "2026-01-23T10:21:00Z"
}
```

### Get Shipment Timeline

**Request:**
```bash
curl -X GET http://localhost:8084/api/v1/shipments/{shipmentId}/timeline \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "shipmentId": "shp-98765432-wxyz-uvst-rqpo-nmlkjihgfedcba",
    "events": [
      {
        "eventId": "evt-001",
        "eventType": "CREATED",
        "eventTime": "2026-01-22T08:00:00Z",
        "source": "SYSTEM"
      },
      {
        "eventId": "evt-002",
        "eventType": "PICKED_UP",
        "eventTime": "2026-01-22T10:30:00Z",
        "location": {
          "lat": 12.9716,
          "lon": 77.5946,
          "hubCode": "BLR-HUB-01"
        },
        "source": "SCANNER"
      },
      {
        "eventId": "evt-003",
        "eventType": "IN_TRANSIT",
        "eventTime": "2026-01-23T10:20:30Z",
        "location": {
          "lat": 15.3173,
          "lon": 75.7139
        },
        "source": "GPS_DEVICE"
      }
    ]
  },
  "timestamp": "2026-01-23T10:21:00Z"
}
```

### Search Shipments

**Request:**
```bash
curl -X GET "http://localhost:8084/api/v1/shipments?status=IN_TRANSIT&page=0&size=20" \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

---

## Prediction Engine Service

Base URL: `http://localhost:8085/api/v1`

### Get Shipment Predictions

**Request:**
```bash
curl -X GET http://localhost:8085/api/v1/predictions/{shipmentId} \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "shipmentId": "shp-98765432-wxyz-uvst-rqpo-nmlkjihgfedcba",
    "tenantId": "123e4567-e89b-12d3-a456-426614174000",
    "eta": "2026-01-25T14:30:00Z",
    "etaConfidence": 0.85,
    "delayProbability": 0.15,
    "delayRisk": "LOW",
    "anomalyDetected": false,
    "anomalyFlags": [],
    "factors": {
      "distanceRemaining": 450.5,
      "averageSpeed": 55.2,
      "historicalOnTime": 0.92,
      "weatherImpact": 0.05
    },
    "updatedAt": "2026-01-23T10:20:35Z"
  },
  "timestamp": "2026-01-23T10:21:00Z"
}
```

---

### Alert Management (Prediction Service)

#### List Alerts

**Request:**
```bash
curl -X GET "http://localhost:8085/api/v1/alerts?page=0&size=20&sortBy=createdAt&sortDir=desc" \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

#### Get Alert Details

**Request:**
```bash
curl -X GET http://localhost:8085/api/v1/alerts/{alertId} \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

#### Acknowledge Alert

**Request:**
```bash
curl -X POST http://localhost:8085/api/v1/alerts/{alertId}/acknowledge \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

#### Resolve Alert

**Request:**
```bash
curl -X POST http://localhost:8085/api/v1/alerts/{alertId}/resolve \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "comment": "Issue resolved manually"
  }'
```

#### Bulk Acknowledge

**Request:**
```bash
curl -X POST http://localhost:8085/api/v1/alerts/bulk/acknowledge \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "alertIds": ["uuid1", "uuid2"]
  }'
```

#### Alert Stream (SSE)

**Request:**
```bash
curl -N -H "Accept: text/event-stream" \
  http://localhost:8085/api/v1/alerts/stream \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

---

## Error Responses

All APIs use a consistent error response format:

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Request validation failed",
    "fieldErrors": {
      "email": "Invalid email format",
      "password": "Password must be at least 8 characters"
    }
  },
  "timestamp": "2026-01-23T10:00:00Z",
  "correlationId": "corr-12345678-abcd-efgh-ijkl-mnopqrstuvwx"
}
```

### Common Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `VALIDATION_FAILED` | 400 | Request validation failed |
| `UNAUTHORIZED` | 401 | Authentication required |
| `FORBIDDEN` | 403 | Access denied |
| `RESOURCE_NOT_FOUND` | 404 | Resource not found |
| `DUPLICATE_RESOURCE` | 409 | Resource already exists |
| `INTERNAL_ERROR` | 500 | Internal server error |

---

## Seed Demo Data

To create demo data for testing, use the seed endpoint:

```bash
curl -X POST http://localhost:8081/api/v1/seed/demo
```

This creates:
- Demo tenant: `demo-tenant`
- Admin user: `admin@demo.com` / `admin123`
- Operations user: `ops@demo.com` / `ops123`
- Viewer: `viewer@demo.com` / `viewer123`
