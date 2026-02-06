# STRIDE Threat Model - SupplySight

## Overview

This document analyzes security threats to SupplySight using the STRIDE framework.

## System Components

```
┌─────────────────┐     ┌──────────────┐     ┌─────────────────┐
│   Frontend      │────▶│  API Gateway │────▶│    Services     │
│   (React SPA)   │     │  (Spring)    │     │ (Spring Boot)   │
└─────────────────┘     └──────────────┘     └─────────────────┘
                               │                      │
                               ▼                      ▼
                        ┌──────────────┐     ┌─────────────────┐
                        │    Redis     │     │   PostgreSQL    │
                        │   (Cache)    │     │   (Database)    │
                        └──────────────┘     └─────────────────┘
```

---

## STRIDE Analysis

### S - Spoofing (Identity)

| Threat | Risk | Mitigation |
|--------|------|------------|
| Stolen JWT tokens | HIGH | Short expiration (15 min), refresh token rotation |
| Forged tenant IDs | HIGH | Tenant ID from JWT claims, not user input |
| API key compromise | MEDIUM | Key rotation capability, key scoping |
| Session hijacking | MEDIUM | Secure cookies, SameSite attribute |

**Residual Risk**: MEDIUM - Further strengthen with device fingerprinting

---

### T - Tampering

| Threat | Risk | Mitigation |
|--------|------|------------|
| Modified request payloads | MEDIUM | Input validation, schema enforcement |
| SQL injection | LOW | Parameterized queries via JPA |
| XSS in user content | LOW | CSP headers, output encoding |
| Message tampering (Kafka) | LOW | Internal network, no user access |

**Residual Risk**: LOW - Well-mitigated with existing controls

---

### R - Repudiation

| Threat | Risk | Mitigation |
|--------|------|------------|
| Denied admin actions | MEDIUM | Audit logging with user ID |
| Disputed data changes | MEDIUM | Event sourcing, immutable logs |
| Login activity disputes | LOW | Login event logging with IP/timestamp |

**Residual Risk**: LOW - Comprehensive audit trail

---

### I - Information Disclosure

| Threat | Risk | Mitigation |
|--------|------|------------|
| Cross-tenant data exposure | CRITICAL | Tenant ID in all queries, row-level security |
| API error message leakage | MEDIUM | Generic errors in production |
| Log file data exposure | MEDIUM | Sensitive field redaction |
| Database credential theft | HIGH | Environment injection, no hardcoding |

**Residual Risk**: MEDIUM - Cross-tenant requires constant vigilance

---

### D - Denial of Service

| Threat | Risk | Mitigation |
|--------|------|------------|
| API flooding | MEDIUM | Rate limiting per tenant |
| Resource exhaustion | MEDIUM | Connection pools, timeout limits |
| SSE connection exhaustion | MEDIUM | Per-tenant SSE limits |
| Large payload attacks | LOW | Request size limits |

**Residual Risk**: LOW - Well-protected with quotas

---

### E - Elevation of Privilege

| Threat | Risk | Mitigation |
|--------|------|------------|
| Admin API access by users | HIGH | @PreAuthorize("hasRole('ADMIN')") |
| Cross-tenant access | CRITICAL | TenantContext validation everywhere |
| JWT role manipulation | HIGH | Server-side role verification |
| Dependency vulnerabilities | MEDIUM | Regular dependency scanning |

**Residual Risk**: MEDIUM - Requires ongoing vigilance

---

## Risk Summary

| Category | Overall Risk |
|----------|-------------|
| Spoofing | MEDIUM |
| Tampering | LOW |
| Repudiation | LOW |
| Information Disclosure | MEDIUM |
| Denial of Service | LOW |
| Elevation of Privilege | MEDIUM |

---

## Recommended Actions

1. **Critical**: Add automated cross-tenant access testing
2. **High**: Implement dependency vulnerability scanning in CI/CD
3. **Medium**: Add device/browser fingerprinting for session security
4. **Medium**: Implement secrets manager integration (HashiCorp Vault)
5. **Low**: Add CAPTCHA for public-facing forms
