-- V7: matches
-- Immutable record created by MatchService when all three conditions are satisfied:
--   1. candidate swiped LIKE on the job
--   2. candidate submitted an application for the job
--   3. employer swiped LIKE on the candidate's application
--
-- uq_match_candidate_job is the idempotency guard.
-- The Kafka consumer uses INSERT ... ON CONFLICT DO NOTHING to safely handle
-- duplicate match-creation attempts without raising exceptions.

CREATE TABLE matches (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id   UUID        NOT NULL,
    employer_id    UUID        NOT NULL,
    job_id         UUID        NOT NULL,
    application_id UUID        NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                               CHECK (status IN ('ACTIVE', 'ARCHIVED', 'EXPIRED')),
    matched_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_match_candidate
        FOREIGN KEY (candidate_id)   REFERENCES users (id),
    CONSTRAINT fk_match_employer
        FOREIGN KEY (employer_id)    REFERENCES users (id),
    CONSTRAINT fk_match_job
        FOREIGN KEY (job_id)         REFERENCES jobs (id),
    CONSTRAINT fk_match_application
        FOREIGN KEY (application_id) REFERENCES applications (id),
    CONSTRAINT uq_match_candidate_job
        UNIQUE (candidate_id, job_id)
);

-- Candidate's match feed — sorted newest first
CREATE INDEX idx_matches_candidate_id ON matches (candidate_id, matched_at DESC);
-- Employer's match feed — sorted newest first
CREATE INDEX idx_matches_employer_id  ON matches (employer_id,  matched_at DESC);
-- Job-level match lookup (admin / analytics)
CREATE INDEX idx_matches_job_id       ON matches (job_id);
