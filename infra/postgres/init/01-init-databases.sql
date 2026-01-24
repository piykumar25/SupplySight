-- Initialize databases for each service
-- All services share the same database with schema separation

-- Create extension for UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create schemas for each service
CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS tracking;
CREATE SCHEMA IF NOT EXISTS visibility;
CREATE SCHEMA IF NOT EXISTS prediction;
CREATE SCHEMA IF NOT EXISTS audit;

-- Grant permissions
GRANT ALL ON SCHEMA identity TO supplysight;
GRANT ALL ON SCHEMA tracking TO supplysight;
GRANT ALL ON SCHEMA visibility TO supplysight;
GRANT ALL ON SCHEMA prediction TO supplysight;
GRANT ALL ON SCHEMA audit TO supplysight;

-- Create indexes function for tenant isolation
CREATE OR REPLACE FUNCTION create_tenant_index(table_name text, schema_name text DEFAULT 'public')
RETURNS void AS $$
BEGIN
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_tenant_id ON %I.%I(tenant_id)', 
                   table_name, schema_name, table_name);
END;
$$ LANGUAGE plpgsql;

-- Log initialization
DO $$
BEGIN
    RAISE NOTICE 'Database initialization complete';
END $$;
