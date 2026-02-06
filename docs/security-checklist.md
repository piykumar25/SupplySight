# SupplySight Security Checklist

## Authentication & Authorization

### JWT Security
- [x] JWT tokens have reasonable expiration (15 min access, 7 day refresh)
- [x] Refresh tokens stored securely (httpOnly cookies preferred)
- [x] JWT secret is environment-injected, not hardcoded
- [x] Token validation on every protected endpoint
- [x] Role-based access control (RBAC) enforced

### Session Management
- [ ] Implement session invalidation on logout
- [ ] Add concurrent session limit per user
- [ ] Implement session timeout for inactive users

## Input Validation

### API Endpoints
- [x] Request body validation with Jakarta Bean Validation
- [x] Path variable validation (UUID format, etc.)
- [x] Query parameter bounds checking
- [x] SQL injection prevention via parameterized queries (JPA)

### File Uploads (if applicable)
- [ ] File type validation (whitelist approach)
- [ ] File size limits enforced
- [ ] Virus scanning for uploaded files

## Transport Security

### HTTPS/TLS
- [x] TLS 1.2+ required in production
- [x] HSTS header configured
- [x] Secure cookie flags (Secure, SameSite)

### API Gateway
- [x] Rate limiting configured
- [x] Request size limits
- [x] Timeout configurations

## Application Security

### Headers
- [x] Content-Security-Policy (CSP) configured
- [x] X-Content-Type-Options: nosniff
- [x] X-Frame-Options: SAMEORIGIN
- [x] X-XSS-Protection: 1; mode=block
- [x] Referrer-Policy: strict-origin-when-cross-origin

### Error Handling
- [x] Generic error messages in production
- [x] No stack traces exposed externally
- [x] Correlation IDs for internal debugging

## Data Protection

### Sensitive Data
- [x] Passwords hashed with bcrypt
- [x] Sensitive fields not logged
- [x] PII access audited

### Database
- [x] Least-privilege database users
- [x] Multi-tenant data isolation
- [x] Encrypted connections

## Infrastructure

### Dependencies
- [ ] Run OWASP Dependency Check regularly
- [ ] No known critical CVEs in dependencies
- [ ] Dependencies up-to-date

### Secrets Management
- [x] No secrets in source code
- [x] Environment variables for configuration
- [ ] Consider secrets manager (Vault, AWS Secrets Manager)

### Logging & Monitoring
- [x] Security events logged
- [x] Failed login attempts tracked
- [x] Admin actions audited

## Compliance

### GDPR (if applicable)
- [ ] Data retention policies implemented
- [ ] Right to deletion supported
- [ ] Data export capability

### Tenant Isolation
- [x] Tenant context validated on every request
- [x] Cross-tenant access prevented
- [x] Tenant ID in all audit logs
