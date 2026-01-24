package com.supplysight.common.security;

/**
 * Application roles for RBAC.
 */
public enum Role {
    /**
     * System administrator with full access.
     */
    ADMIN,
    
    /**
     * Operations user with create/update access.
     */
    OPS_USER,
    
    /**
     * Viewer with read-only access.
     */
    VIEWER;

    /**
     * Check if this role has at least the specified role's privileges.
     */
    public boolean hasAtLeast(Role required) {
        return this.ordinal() <= required.ordinal();
    }
}
