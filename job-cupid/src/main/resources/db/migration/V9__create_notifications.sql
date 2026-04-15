-- V9: notifications
-- In-app notification records created by Kafka consumers in NotificationService.
-- External dispatch (email, push) is handled separately via the notification.events topic.
-- ON DELETE CASCADE means user deletion cleans up notifications automatically.

CREATE TABLE notifications (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID         NOT NULL,
    type           VARCHAR(50)  NOT NULL
                                CHECK (type IN (
                                    'MATCH_CREATED',
                                    'APPLICATION_RECEIVED',
                                    'APPLICATION_STATUS_CHANGED',
                                    'SWIPE_RECEIVED',
                                    'SUBSCRIPTION_RENEWED',
                                    'SUBSCRIPTION_EXPIRING'
                                )),
    title          VARCHAR(255) NOT NULL,
    body           TEXT         NOT NULL,
    reference_id   UUID,                    -- e.g. matchId, applicationId
    reference_type VARCHAR(50),             -- e.g. 'MATCH', 'APPLICATION'
    is_read        BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at        TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_notif_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Unread badge count + unread list (most common query path)
-- Partial index: only unread rows, newest first
CREATE INDEX idx_notif_user_unread ON notifications (user_id, created_at DESC)
    WHERE is_read = FALSE;

-- Full notification history (all read/unread), newest first
CREATE INDEX idx_notif_user_all    ON notifications (user_id, created_at DESC);
