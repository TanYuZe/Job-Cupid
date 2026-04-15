-- V1: users table
-- Central identity table for all roles (USER, EMPLOYER, ADMIN).
-- is_premium is a denormalized fast-path; subscriptions table is the source of truth.

CREATE TABLE users (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    role            VARCHAR(20)  NOT NULL
                                 CHECK (role IN ('USER', 'EMPLOYER', 'ADMIN')),
    is_premium      BOOLEAN      NOT NULL DEFAULT FALSE,
    bio             TEXT,
    location        VARCHAR(255),
    photo_url       VARCHAR(500),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    is_banned       BOOLEAN      NOT NULL DEFAULT FALSE,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);

ALTER TABLE users
    ADD CONSTRAINT uq_users_email UNIQUE (email);

-- Partial index: email lookup only considers non-deleted accounts
CREATE INDEX idx_users_email     ON users (email)             WHERE deleted_at IS NULL;
-- Role-based admin queries
CREATE INDEX idx_users_role      ON users (role)              WHERE deleted_at IS NULL;
-- Active/banned status checks
CREATE INDEX idx_users_is_active ON users (is_active, deleted_at);
