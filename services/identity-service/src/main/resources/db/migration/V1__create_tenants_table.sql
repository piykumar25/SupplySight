-- Create identity schema
CREATE SCHEMA IF NOT EXISTS identity;

-- Tenants table
CREATE TABLE identity.tenants (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    address TEXT,
    settings JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0
);

-- Indexes
CREATE INDEX idx_tenants_code ON identity.tenants(code);
CREATE INDEX idx_tenants_status ON identity.tenants(status);

-- Comments
COMMENT ON TABLE identity.tenants IS 'Multi-tenant registry for the platform';
COMMENT ON COLUMN identity.tenants.code IS 'Unique tenant code for identification';
COMMENT ON COLUMN identity.tenants.status IS 'Tenant status: ACTIVE, SUSPENDED, DELETED';
