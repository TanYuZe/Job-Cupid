# Job-Cupid — Entity Relationship Diagram

## Full ERD

```mermaid
erDiagram
    users {
        uuid        id              PK
        varchar     email           UK
        varchar     password_hash
        varchar     first_name
        varchar     last_name
        varchar     role               "USER | EMPLOYER | ADMIN"
        boolean     is_premium         "denormalised from subscriptions"
        text        bio
        varchar     location
        varchar     photo_url
        boolean     is_active
        boolean     is_banned
        timestamptz last_login_at
        timestamptz deleted_at         "NULL = active"
    }

    candidate_profiles {
        uuid        id              PK
        uuid        user_id         FK UK
        varchar     headline
        varchar     resume_url
        text_array  skills
        smallint    years_of_experience
        integer     desired_salary_min
        integer     desired_salary_max
        boolean     preferred_remote
        varchar     preferred_location
        boolean     is_open_to_work
    }

    employer_profiles {
        uuid        id              PK
        uuid        user_id         FK UK
        varchar     company_name
        text        company_description
        varchar     company_website
        varchar     company_logo_url
        varchar     company_size       "1-10 | 11-50 | … | 1000+"
        varchar     industry
        smallint    founded_year
    }

    jobs {
        uuid        id              PK
        uuid        employer_id     FK
        varchar     title
        text        description
        varchar     category
        varchar     location
        boolean     is_remote
        integer     salary_min
        integer     salary_max
        char3       currency           "default USD"
        varchar     employment_type    "FULL_TIME | PART_TIME | CONTRACT | INTERNSHIP | FREELANCE"
        varchar     experience_level   "ENTRY | MID | SENIOR | LEAD | EXECUTIVE"
        text_array  required_skills
        varchar     status             "ACTIVE | PAUSED | CLOSED | ARCHIVED"
        integer     boost_score        "0 = normal, 100 = premium boosted"
        integer     application_count  "atomic denormalised counter"
        tsvector    search_vector      "maintained by DB trigger"
        timestamptz expires_at
        timestamptz deleted_at         "NULL = active"
    }

    candidate_swipes {
        uuid        id              PK
        uuid        candidate_id    FK
        uuid        job_id          FK
        varchar     action             "LIKE | PASS"
        timestamptz swiped_at
    }

    applications {
        uuid        id              PK
        uuid        candidate_id    FK
        uuid        job_id          FK
        text        cover_letter
        varchar     resume_url         "snapshot at apply time"
        varchar     status             "PENDING | REVIEWED | SHORTLISTED | ACCEPTED | REJECTED | WITHDRAWN"
        timestamptz applied_at
        timestamptz reviewed_at
    }

    employer_swipes {
        uuid        id              PK
        uuid        employer_id     FK
        uuid        application_id  FK
        uuid        candidate_id    FK "denormalised for match gate"
        uuid        job_id          FK "denormalised for match gate"
        varchar     action             "LIKE | REJECT"
        timestamptz swiped_at
    }

    matches {
        uuid        id              PK
        uuid        candidate_id    FK
        uuid        employer_id     FK
        uuid        job_id          FK
        uuid        application_id  FK UK
        varchar     status             "ACTIVE | ARCHIVED | EXPIRED"
        timestamptz matched_at
    }

    subscriptions {
        uuid        id              PK
        uuid        user_id         FK
        varchar     plan               "PREMIUM_MONTHLY | PREMIUM_ANNUAL"
        varchar     status             "ACTIVE | CANCELLED | EXPIRED | PAST_DUE"
        varchar     stripe_customer_id
        varchar     stripe_subscription_id
        timestamptz current_period_start
        timestamptz current_period_end
        timestamptz cancelled_at
    }

    notifications {
        uuid        id              PK
        uuid        user_id         FK
        varchar     type               "MATCH_CREATED | APPLICATION_RECEIVED | …"
        varchar     title
        text        body
        uuid        reference_id       "matchId | applicationId | …"
        varchar     reference_type     "MATCH | APPLICATION | …"
        boolean     is_read
        timestamptz read_at
    }

    refresh_tokens {
        uuid        id              PK
        uuid        user_id         FK
        varchar     token_hash      UK "SHA-256 of raw token"
        varchar     device_info
        inet        ip_address
        timestamptz issued_at
        timestamptz expires_at
        timestamptz revoked_at
        boolean     is_revoked
    }

    %% ── Profile relationships (1-to-1) ──────────────────────────────────────
    users ||--o| candidate_profiles : "has candidate profile"
    users ||--o| employer_profiles  : "has employer profile"

    %% ── Job relationships ────────────────────────────────────────────────────
    users ||--o{ jobs               : "employer posts"

    %% ── Swipe relationships ──────────────────────────────────────────────────
    users ||--o{ candidate_swipes   : "candidate swipes"
    jobs  ||--o{ candidate_swipes   : "swiped on"

    %% ── Application relationships ────────────────────────────────────────────
    users        ||--o{ applications    : "candidate applies"
    jobs         ||--o{ applications    : "receives applications"

    %% ── Employer swipe relationships ─────────────────────────────────────────
    users        ||--o{ employer_swipes : "employer reviews"
    applications ||--o{ employer_swipes : "reviewed via"
    jobs         ||--o{ employer_swipes : "reviewed on"

    %% ── Match relationships ───────────────────────────────────────────────────
    users        ||--o{ matches         : "candidate in match"
    users        ||--o{ matches         : "employer in match"
    jobs         ||--o{ matches         : "job matched on"
    applications ||--o| matches         : "results in match"

    %% ── Auth / subscription / notification ───────────────────────────────────
    users ||--o{ subscriptions  : "subscribes"
    users ||--o{ notifications  : "receives"
    users ||--o{ refresh_tokens : "authenticates with"
```

---

## Relationship Summary

| From | To | Cardinality | Via |
|------|----|-------------|-----|
| `users` | `candidate_profiles` | 1 — 0..1 | `user_id` |
| `users` | `employer_profiles` | 1 — 0..1 | `user_id` |
| `users` | `jobs` | 1 — 0..* | `employer_id` |
| `users` | `candidate_swipes` | 1 — 0..* | `candidate_id` |
| `users` | `applications` | 1 — 0..* | `candidate_id` |
| `users` | `employer_swipes` | 1 — 0..* | `employer_id` |
| `users` | `matches` | 1 — 0..* | `candidate_id` **and** `employer_id` |
| `users` | `subscriptions` | 1 — 0..* | `user_id` |
| `users` | `notifications` | 1 — 0..* | `user_id` |
| `users` | `refresh_tokens` | 1 — 0..* | `user_id` |
| `jobs` | `candidate_swipes` | 1 — 0..* | `job_id` |
| `jobs` | `applications` | 1 — 0..* | `job_id` |
| `jobs` | `employer_swipes` | 1 — 0..* | `job_id` *(denorm)* |
| `jobs` | `matches` | 1 — 0..* | `job_id` |
| `applications` | `employer_swipes` | 1 — 0..* | `application_id` |
| `applications` | `matches` | 1 — 0..1 | `application_id` |

---

## Match Gate — How a Match Is Created

Three conditions must all be true before a `matches` row is inserted:

```
candidate_swipes  WHERE candidate_id = X AND job_id = Y AND action = 'LIKE'
applications      WHERE candidate_id = X AND job_id = Y
employer_swipes   WHERE candidate_id = X AND job_id = Y AND action = 'LIKE'
```

A Kafka consumer evaluates all three after each employer swipe.
`INSERT INTO matches ... ON CONFLICT DO NOTHING` ensures idempotency.

---

## Key Unique Constraints

| Table | Unique Constraint | Meaning |
|-------|-------------------|---------|
| `users` | `(email)` | No duplicate accounts |
| `candidate_profiles` | `(user_id)` | One profile per candidate |
| `employer_profiles` | `(user_id)` | One profile per employer |
| `candidate_swipes` | `(candidate_id, job_id)` | One swipe per job per candidate |
| `applications` | `(candidate_id, job_id)` | One application per job |
| `employer_swipes` | `(employer_id, application_id)` | One review per application |
| `matches` | `(candidate_id, job_id)` | One match per (candidate, job) ever |
| `refresh_tokens` | `(token_hash)` | No duplicate tokens |
