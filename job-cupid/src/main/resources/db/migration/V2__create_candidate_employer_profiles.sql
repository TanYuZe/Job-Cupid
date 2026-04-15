-- V2: candidate_profiles and employer_profiles
-- Both are one-to-one extensions of the users table, split by role.
-- Created on first profile update (upsert pattern in service layer).

CREATE TABLE candidate_profiles (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID        NOT NULL,
    headline            VARCHAR(255),
    resume_url          VARCHAR(500),
    skills              TEXT[],
    years_of_experience SMALLINT    CHECK (years_of_experience >= 0),
    desired_salary_min  INTEGER,
    desired_salary_max  INTEGER,
    preferred_remote    BOOLEAN     NOT NULL DEFAULT FALSE,
    preferred_location  VARCHAR(255),
    is_open_to_work     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_cp_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_cp_user
        UNIQUE (user_id),
    CONSTRAINT chk_cp_salary_range CHECK (
        desired_salary_min IS NULL
        OR desired_salary_max IS NULL
        OR desired_salary_min <= desired_salary_max
    )
);

CREATE INDEX idx_cp_user_id      ON candidate_profiles (user_id);
CREATE INDEX idx_cp_skills       ON candidate_profiles USING GIN (skills);
CREATE INDEX idx_cp_open_to_work ON candidate_profiles (is_open_to_work);

-- ---------------------------------------------------------------------------

CREATE TABLE employer_profiles (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID        NOT NULL,
    company_name        VARCHAR(255) NOT NULL,
    company_description TEXT,
    company_website     VARCHAR(500),
    company_logo_url    VARCHAR(500),
    company_size        VARCHAR(50)
                                    CHECK (company_size IN
                                           ('1-10', '11-50', '51-200',
                                            '201-500', '501-1000', '1000+')),
    industry            VARCHAR(100),
    founded_year        SMALLINT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_ep_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_ep_user
        UNIQUE (user_id)
);

CREATE INDEX idx_ep_user_id ON employer_profiles (user_id);
