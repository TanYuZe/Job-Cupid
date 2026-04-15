-- V3: jobs table + full-text search trigger
-- boost_score is denormalized for fast feed sort; updated by SubscriptionService and nightly scheduler.
-- application_count is an atomic counter incremented on each application to avoid aggregate queries.
-- search_vector is maintained automatically by trig_jobs_search_vector on INSERT/UPDATE.

CREATE TABLE jobs (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    employer_id      UUID         NOT NULL,
    title            VARCHAR(255) NOT NULL,
    description      TEXT         NOT NULL,
    category         VARCHAR(100) NOT NULL,
    location         VARCHAR(255),
    is_remote        BOOLEAN      NOT NULL DEFAULT FALSE,
    salary_min       INTEGER,
    salary_max       INTEGER,
    currency         CHAR(3)      NOT NULL DEFAULT 'USD',
    employment_type  VARCHAR(50)  NOT NULL
                                  CHECK (employment_type IN
                                         ('FULL_TIME', 'PART_TIME', 'CONTRACT',
                                          'INTERNSHIP', 'FREELANCE')),
    experience_level VARCHAR(50)
                                  CHECK (experience_level IN
                                         ('ENTRY', 'MID', 'SENIOR', 'LEAD', 'EXECUTIVE')),
    required_skills  TEXT[],
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                                  CHECK (status IN ('ACTIVE', 'PAUSED', 'CLOSED', 'ARCHIVED')),
    boost_score      INTEGER      NOT NULL DEFAULT 0,
    application_count INTEGER     NOT NULL DEFAULT 0,
    search_vector    TSVECTOR,
    expires_at       TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMPTZ,

    CONSTRAINT fk_jobs_employer
        FOREIGN KEY (employer_id) REFERENCES users (id),
    CONSTRAINT chk_jobs_salary CHECK (
        salary_min IS NULL
        OR salary_max IS NULL
        OR salary_min <= salary_max
    )
);

-- Employer's own job listing
CREATE INDEX idx_jobs_employer_id ON jobs (employer_id)
    WHERE deleted_at IS NULL;

-- Status filter (admin / feed)
CREATE INDEX idx_jobs_status      ON jobs (status)
    WHERE deleted_at IS NULL;

-- Category browse filter
CREATE INDEX idx_jobs_category    ON jobs (category)
    WHERE status = 'ACTIVE' AND deleted_at IS NULL;

-- Location browse filter
CREATE INDEX idx_jobs_location    ON jobs (location)
    WHERE status = 'ACTIVE' AND deleted_at IS NULL;

-- Remote toggle filter
CREATE INDEX idx_jobs_remote      ON jobs (is_remote)
    WHERE status = 'ACTIVE';

-- Composite feed index: drives keyset pagination (boost_score DESC, created_at DESC, id DESC)
CREATE INDEX idx_jobs_feed        ON jobs (boost_score DESC, created_at DESC, id DESC)
    WHERE status = 'ACTIVE' AND deleted_at IS NULL;

-- Full-text search (GIN on tsvector column — maintained by trigger below)
CREATE INDEX idx_jobs_search      ON jobs USING GIN (search_vector);

-- Skills array overlap filter (premium advanced filter)
CREATE INDEX idx_jobs_skills      ON jobs USING GIN (required_skills);

-- Salary range filter
CREATE INDEX idx_jobs_salary      ON jobs (salary_min, salary_max)
    WHERE status = 'ACTIVE';

-- ---------------------------------------------------------------------------
-- Full-text search trigger
-- Weights: title (A) > category (B) > description (C)
-- ---------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION jobs_search_vector_update()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('english', COALESCE(NEW.title, '')),       'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.category, '')),    'B') ||
        setweight(to_tsvector('english', COALESCE(NEW.description, '')), 'C');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trig_jobs_search_vector
    BEFORE INSERT OR UPDATE ON jobs
    FOR EACH ROW EXECUTE FUNCTION jobs_search_vector_update();
