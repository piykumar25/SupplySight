package com.supplysight.common.security;

import java.util.UUID;

/**
 * Thread-local holder for tenant context extracted from JWT. Ensures tenant isolation across all
 * operations.
 */
public final class TenantContext {

    private static final ThreadLocal<TenantInfo> CONTEXT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(TenantInfo tenantInfo) {
        CONTEXT.set(tenantInfo);
    }

    public static TenantInfo get() {
        return CONTEXT.get();
    }

    public static UUID getTenantId() {
        TenantInfo info = CONTEXT.get();
        return info != null ? info.tenantId() : null;
    }

    public static UUID getUserId() {
        TenantInfo info = CONTEXT.get();
        return info != null ? info.userId() : null;
    }

    public static void clear() {
        CONTEXT.remove();
    }

    // Compatibility methods
    public static java.util.Optional<UUID> getCurrentTenant() {
        return java.util.Optional.ofNullable(getTenantId());
    }

    public static java.util.Optional<TenantInfo> getCurrentTenantInfo() {
        return java.util.Optional.ofNullable(get());
    }

    /** Tenant and user information extracted from JWT. */
    public record TenantInfo(
            UUID tenantId, UUID userId, String username, java.util.Set<String> roles) {}
}
