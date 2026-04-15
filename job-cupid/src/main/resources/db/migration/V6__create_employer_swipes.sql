-- V6: employer_swipes
-- Must come after V5 (applications) because of the FK to applications(id).
-- One record per (employer, application). Re-swiping overwrites the action.
--
-- candidate_id and job_id are denormalized from the referenced application row
-- so the three-condition match gate in MatchService can evaluate all conditions
-- with index-only scans — no join with applications required at evaluation time.
--
-- idx_es_match_gate is the third leg of the match gate:
--   "Did this employer LIKE this candidate for this job?"

CREATE TABLE employer_swipes (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    employer_id    UUID        NOT NULL,
    application_id UUID        NOT NULL,
    candidate_id   UUID        NOT NULL,   -- denormalized from applications
    job_id         UUID        NOT NULL,   -- denormalized from applications
    action         VARCHAR(10) NOT NULL CHECK (action IN ('LIKE', 'REJECT')),
    swiped_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_es_employer
        FOREIGN KEY (employer_id)    REFERENCES users (id),
    CONSTRAINT fk_es_application
        FOREIGN KEY (application_id) REFERENCES applications (id),
    CONSTRAINT fk_es_candidate
        FOREIGN KEY (candidate_id)   REFERENCES users (id),
    CONSTRAINT fk_es_job
        FOREIGN KEY (job_id)         REFERENCES jobs (id),
    CONSTRAINT uq_es_employer_app
        UNIQUE (employer_id, application_id)
);

-- General employer swipe history
CREATE INDEX idx_es_employer_id    ON employer_swipes (employer_id);
-- Look up swipe by application
CREATE INDEX idx_es_application_id ON employer_swipes (application_id);
-- Match gate condition 3 — partial index, index-only scan
CREATE INDEX idx_es_match_gate     ON employer_swipes (candidate_id, job_id)
    WHERE action = 'LIKE';
