-- Refresh tokens table for token management
CREATE TABLE identity.refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    device_info VARCHAR(500),
    ip_address VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_refresh_tokens_user_id ON identity.refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON identity.refresh_tokens(expires_at);
CREATE INDEX idx_refresh_tokens_revoked ON identity.refresh_tokens(revoked);

-- Cleanup function for expired tokens
CREATE OR REPLACE FUNCTION identity.cleanup_expired_tokens()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM identity.refresh_tokens 
    WHERE expires_at < CURRENT_TIMESTAMP OR revoked = TRUE;
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Comments
COMMENT ON TABLE identity.refresh_tokens IS 'Refresh tokens for JWT token rotation';
COMMENT ON FUNCTION identity.cleanup_expired_tokens IS 'Removes expired and revoked refresh tokens';
