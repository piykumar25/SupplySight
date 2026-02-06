# Error Budget Policy

## Overview

This document defines what happens when SupplySight's error budget is exhausted or running low.

## SLO Targets

| SLI | Target | Error Budget (monthly) |
|-----|--------|----------------------|
| API Availability | 99.9% | 43.2 minutes downtime |
| Visibility API p95 Latency | < 300ms | 5% of requests may exceed |
| Alert Delivery | < 5 seconds | 5% may be delayed |
| SSE Connection Success | 99% | 1% may fail |

## Error Budget States

### 🟢 Healthy (>50% remaining)
- Normal operations
- Feature development continues
- Standard change velocity

### 🟡 Caution (25-50% remaining)
- Alert: `SLOErrorBudgetLow`
- Increase deployment testing rigor
- Defer high-risk changes
- Review recent changes for contributing factors

### 🟠 At Risk (<25% remaining)
- Alert: `SLOErrorBudgetSlowBurn`
- Feature freeze for non-critical changes
- Focus on stability improvements
- Conduct incident review
- On-call staffing increase

### 🔴 Exhausted (0% remaining)
- Alert: `SLOErrorBudgetExhausted`
- **Complete feature freeze**
- All engineering effort on reliability
- Rollback recent risky changes
- Executive escalation required
- Daily standups on reliability progress

## Recovery Actions

### When Budget Exhausted

1. **Immediate** (first 2 hours)
   - Page on-call engineer and engineering lead
   - Identify top error contributors via dashboards
   - Rollback any changes from last 48 hours
   - Notify stakeholders

2. **Short-term** (first week)
   - Root cause analysis for all incidents
   - Implement quick fixes
   - Increase monitoring granularity
   - Daily reliability standups

3. **Medium-term** (2-4 weeks)
   - Address systemic reliability issues
   - Improve test coverage
   - Enhance canary deployments
   - Conduct post-mortem reviews

## Budget Reset

Error budget resets on the 1st of each month at 00:00 UTC.

## Exceptions

Emergency security fixes are exempt from feature freeze, but require:
- VP-level approval
- Extra testing and staging validation
- Immediate rollback capability
- Enhanced monitoring during rollout

## Contacts

- **SRE Lead**: Escalation for error budget decisions
- **Engineering Lead**: Feature freeze approval
- **VP Engineering**: Emergency exceptions
