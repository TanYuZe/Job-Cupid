-- V8: subscriptions
-- Source of truth for premium status. users.is_premium is a denormalized
-- fast-path kept in sync by SubscriptionService.syncPremiumFlag() and a
-- nightly expiry scheduler job.
--
-- A user may have multiple subscription rows over time (one per billing cycle).
-- SubscriptionService.isActive() queries the most recent ACTIVE row where
-- current_period_end > NOW().

CREATE TABLE subscriptions (
    id                     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                UUID         NOT NULL,
    plan                   VARCHAR(50)  NOT NULL
                                        CHECK (plan IN ('PREMIUM_MONTHLY', 'PREMIUM_ANNUAL')),
    status                 VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                                        CHECK (status IN ('ACTIVE', 'CANCELLED', 'EXPIRED', 'PAST_DUE')),
    stripe_customer_id     VARCHAR(255),
    stripe_subscription_id VARCHAR(255),
    current_period_start   TIMESTAMPTZ  NOT NULL,
    current_period_end     TIMESTAMPTZ  NOT NULL,
    cancelled_at           TIMESTAMPTZ,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_sub_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

-- User's subscription history + status filter
CREATE INDEX idx_sub_user_id       ON subscriptions (user_id, status);
-- Stripe webhook lookup by Stripe subscription ID
CREATE INDEX idx_sub_stripe_sub_id ON subscriptions (stripe_subscription_id);
-- Nightly expiry scheduler: find all ACTIVE subs past their end date
CREATE INDEX idx_sub_expiry        ON subscriptions (current_period_end)
    WHERE status = 'ACTIVE';
