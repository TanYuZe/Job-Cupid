-- V5: applications
-- Bridge table between candidates and jobs.
-- A candidate may only have one application per job (enforced by uq_app_candidate_job).
-- The service layer validates that the candidate swiped LIKE before allowing apply.
-- application_count on jobs is incremented atomically on each successful application.

CREATE TABLE applications (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id UUID        NOT NULL,
    job_id       UUID        NOT NULL,
    cover_letter TEXT,
    resume_url   VARCHAR(500),             -- snapshot of resume at time of apply
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                             CHECK (status IN
                                    ('PENDING', 'REVIEWED', 'SHORTLISTED',
                                     'ACCEPTED', 'REJECTED', 'WITHDRAWN')),
    applied_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reviewed_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_app_candidate
        FOREIGN KEY (candidate_id) REFERENCES users (id),
    CONSTRAINT fk_app_job
        FOREIGN KEY (job_id) REFERENCES jobs (id),
    CONSTRAINT uq_app_candidate_job
        UNIQUE (candidate_id, job_id)
);

-- Candidate's own application history
CREATE INDEX idx_app_candidate_id ON applications (candidate_id);
-- Employer's applicant list for a job
CREATE INDEX idx_app_job_id       ON applications (job_id);
-- Status filter on employer's applicant view
CREATE INDEX idx_app_status       ON applications (job_id, status);
-- Chronological applicant list (newest first)
CREATE INDEX idx_app_applied_at   ON applications (job_id, applied_at DESC);
