# Tenant Onboarding Guide

## Overview

This guide walks through the process of onboarding a new tenant to SupplySight.

---

## Step 1: Create Tenant

### Via API
```bash
curl -X POST http://localhost:8080/api/v1/tenants \
  -H "Authorization: Bearer {admin-token}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Acme Logistics",
    "contactEmail": "admin@acme.com"
  }'
```

### Response
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Acme Logistics",
    "apiKey": "sk_live_xxx..."
  }
}
```

---

## Step 2: Configure Quotas

Default quotas are applied automatically. To customize:

```bash
curl -X PUT http://localhost:8080/admin/tenants/{tenant-id}/limits \
  -H "Authorization: Bearer {admin-token}" \
  -H "Content-Type: application/json" \
  -d '{
    "maxActiveShipments": 5000,
    "maxEventsPerSecond": 200,
    "maxSseConnections": 100
  }'
```

---

## Step 3: Create Admin User

```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer {admin-token}" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "tenant-admin@acme.com",
    "password": "SecurePassword123!",
    "role": "ADMIN",
    "tenantId": "{tenant-id}"
  }'
```

---

## Step 4: Provide API Credentials

Share with tenant:
- **API Endpoint**: `https://api.supplysight.com`
- **API Key**: `sk_live_xxx...`
- **Dashboard URL**: `https://app.supplysight.com`
- **Documentation**: `https://docs.supplysight.com`

---

## Step 5: Integration Checklist

Send to tenant for their integration team:

- [ ] Register webhook endpoints (optional)
- [ ] Configure event ingestion pipeline
- [ ] Set up SSE connection for real-time alerts
- [ ] Test with sandbox shipment

---

## Step 6: Validation

Run onboarding validation:

```bash
# Check tenant exists
curl http://localhost:8080/api/v1/tenants/{tenant-id} \
  -H "Authorization: Bearer {tenant-api-key}"

# Send test event
curl -X POST http://localhost:8080/api/v1/events \
  -H "Authorization: Bearer {tenant-api-key}" \
  -H "Content-Type: application/json" \
  -d '{
    "shipmentId": "TEST-001",
    "eventType": "CREATED",
    "location": {"lat": 40.7128, "lon": -74.0060}
  }'

# Verify shipment created
curl http://localhost:8080/api/v1/shipments \
  -H "Authorization: Bearer {tenant-api-key}"
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| 401 Unauthorized | Check API key is correct |
| 403 Forbidden | Verify tenant ID matches token |
| 429 Rate Limited | Check quota limits |
| 500 Error | Check service logs |

---

## Support Escalation

1. **Tier 1**: Check logs and dashboards
2. **Tier 2**: Review tenant configuration
3. **Tier 3**: Engineering escalation
