-- V10: refresh_tokens
-- Durable audit log for refresh token issuance and revocation.
-- Redis is the primary store (fast lookup); this table is the fallback for:
--   - Auditing which devices have active sessions
--   - Mass-revocation (e.g. ban a user — mark all their tokens is_revoked=true)
--   - Recovery if Redis is flushed
--
-- token_hash stores the SHA-256 of the raw token — never the raw token itself.

CREATE TABLE refresh_tokens (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL,
    token_hash  VARCHAR(255) NOT NULL,   -- SHA-256 of raw token
    device_info VARCHAR(500),
    ip_address  INET,
    issued_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked_at  TIMESTAMPTZ,
    is_revoked  BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_rt_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_rt_token_hash
        UNIQUE (token_hash)
);

-- All active sessions for a user (device management / mass-revoke)
CREATE INDEX idx_rt_user_id    ON refresh_tokens (user_id)
    WHERE is_revoked = FALSE;

-- Token lookup on refresh request (primary lookup path)
CREATE INDEX idx_rt_token_hash ON refresh_tokens (token_hash);

-- Nightly cleanup: find expired tokens to purge
CREATE INDEX idx_rt_expiry     ON refresh_tokens (expires_at)
    WHERE is_revoked = FALSE;
