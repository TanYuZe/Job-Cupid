-- V4: candidate_swipes
-- One record per (candidate, job). Re-swiping the same job overwrites the action
-- via ON CONFLICT DO UPDATE in the service layer (idempotent).
--
-- The partial index idx_cs_action_like is the first leg of the three-condition
-- match gate evaluated by MatchService.

CREATE TABLE candidate_swipes (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id UUID        NOT NULL,
    job_id       UUID        NOT NULL,
    action       VARCHAR(10) NOT NULL CHECK (action IN ('LIKE', 'PASS')),
    swiped_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_cs_candidate
        FOREIGN KEY (candidate_id) REFERENCES users (id),
    CONSTRAINT fk_cs_job
        FOREIGN KEY (job_id) REFERENCES jobs (id),
    CONSTRAINT uq_cs_candidate_job
        UNIQUE (candidate_id, job_id)
);

-- General candidate swipe history
CREATE INDEX idx_cs_candidate_id ON candidate_swipes (candidate_id);
-- All swipes on a specific job (employer "who liked" query, premium feature)
CREATE INDEX idx_cs_job_id       ON candidate_swipes (job_id);
-- Match gate condition 1 — partial index makes this an index-only scan
CREATE INDEX idx_cs_action_like  ON candidate_swipes (candidate_id, job_id)
    WHERE action = 'LIKE';
