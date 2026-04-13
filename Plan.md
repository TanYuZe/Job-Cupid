# Job-Cupid: System Design Plan

## 1. System Overview

Job-Cupid is a double-sided job matching platform modeled after swipe-based dating apps. It connects Candidates and Employers through a three-condition match gate: a candidate must LIKE a job, apply to it, and the employer must LIKE the candidate back. No partial matches are ever surfaced to either party.

### Key Design Principles
- **Event-driven**: All state transitions (swipe, apply, match) emit Kafka events consumed by downstream services
- **Clean Architecture**: Domain logic is isolated from infrastructure concerns (repositories, controllers, messaging)
- **Defense in depth**: JWT + role-based access control + rate limiting via Redis
- **Premium as a first-class concern**: Entitlement checks are centralized in a `SubscriptionService`, not scattered across controllers

---

## 2. Architecture Diagram

```
                        ┌─────────────────────────────────────────────────┐
                        │                   Clients                        │
                        │        (Web / Mobile / Admin Portal)             │
                        └───────────────────┬─────────────────────────────┘
                                            │ HTTPS
                        ┌───────────────────▼─────────────────────────────┐
                        │              API Gateway / Load Balancer         │
                        │         (AWS ALB + Kong / Spring Cloud GW)       │
                        └──────┬─────────────────────────┬────────────────┘
                               │                         │
              ┌────────────────▼──────┐    ┌─────────────▼──────────────┐
              │   Auth Service         │    │   Core API Service          │
              │  (JWT issue/refresh)   │    │  (Spring Boot monolith)     │
              │                        │    │                             │
              │  /auth/**              │    │  /api/v1/**                 │
              └────────────────────────┘    └──────────────┬─────────────┘
                                                           │
                    ┌──────────────────────────────────────┼────────────────────────────────────┐
                    │                                       │                                    │
       ┌────────────▼──────────┐           ┌───────────────▼──────────┐      ┌──────────────────▼──────┐
       │      PostgreSQL        │           │         Redis             │      │     Apache Kafka          │
       │  (Primary data store)  │           │  - Session / JWT deny    │      │  - swipe.events           │
       │  AWS RDS Multi-AZ      │           │  - Rate limit counters   │      │  - application.events     │
       │                        │           │  - Job/profile cache     │      │  - match.events           │
       └────────────────────────┘           │  - Leaderboard/boost     │      │  - notification.events    │
                                            └──────────────────────────┘      └───────────────────────────┘
                                                                                          │
                                                                        ┌─────────────────▼──────────────┐
                                                                        │      Notification Worker         │
                                                                        │  (Kafka consumer → push/email)  │
                                                                        └─────────────────────────────────┘

       ┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
       │                                   AWS Infrastructure                                              │
       │  EKS (K8s pods)  |  RDS (Postgres)  |  ElastiCache (Redis)  |  MSK (Kafka)  |  S3 (assets)     │
       └──────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Services and Responsibilities

### 3.1 Auth Service (module: `auth`)
- Issue JWT access tokens (15 min TTL) and refresh tokens (7 days TTL)
- Store refresh tokens in Redis with user ID mapping
- Blacklist invalidated tokens in Redis on logout
- Password hashing with BCrypt (cost factor 12)

### 3.2 User Service (module: `user`)
- CRUD for candidate and employer profiles
- Profile photo upload to S3 (pre-signed URL flow)
- Role management (USER, EMPLOYER, ADMIN, PREMIUM)

### 3.3 Job Service (module: `job`)
- CRUD for job postings (employer-only)
- Full-text search via PostgreSQL `tsvector` + GIN index
- Filter by: category, location, remote/onsite, salary range
- Soft-delete jobs (status = CLOSED/ARCHIVED)
- Boosted jobs (premium employers) surface first in feed

### 3.4 Swipe Service (module: `swipe`)
- Candidate: LIKE or PASS on a job
- Employer: LIKE or REJECT on a candidate who applied
- Enforce daily swipe limits for non-premium users via Redis counter (TTL = midnight reset)
- Publish `SwipeEvent` to Kafka topic `swipe.events`

### 3.5 Application Service (module: `application`)
- Candidate submits application to a specific job
- Validate that the candidate previously swiped LIKE on the job
- Store application status: PENDING → REVIEWED → ACCEPTED / REJECTED
- Publish `ApplicationEvent` to Kafka topic `application.events`

### 3.6 Match Service (module: `match`)
- Kafka consumer on `swipe.events` and `application.events`
- Evaluate three-condition match gate on every relevant event
- Create match record and publish `MatchEvent` to `match.events`
- Premium feature: expose "who liked you" only to premium users

### 3.7 Notification Service (module: `notification`)
- Kafka consumer on `match.events` and `application.events`
- Send in-app notifications (stored in DB), push notifications, and email via SES

### 3.8 Subscription Service (module: `subscription`)
- Manage premium plans and expiry
- Central `SubscriptionService.isActive(userId)` used across all feature checks
- Stripe webhook integration for payment events

### 3.9 Admin Service (module: `admin`)
- User management (ban, role promotion)
- Job moderation (force-close listings)
- Metrics dashboards via Actuator + Prometheus

---

## 4. API Endpoints

### Auth
| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/auth/register` | Register candidate or employer | Public |
| POST | `/auth/login` | Obtain JWT + refresh token | Public |
| POST | `/auth/refresh` | Rotate refresh token | Refresh token |
| POST | `/auth/logout` | Blacklist token | Bearer |

### Users
| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/v1/users/me` | Get own profile | Bearer |
| PUT | `/api/v1/users/me` | Update own profile | Bearer |
| DELETE | `/api/v1/users/me` | Soft-delete account | Bearer |
| POST | `/api/v1/users/me/photo` | Upload profile photo (S3 pre-sign) | Bearer |
| GET | `/api/v1/users/{id}` | Get user (admin only) | ADMIN |

### Jobs
| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/v1/jobs` | Browse/filter job feed | Bearer (USER) |
| GET | `/api/v1/jobs/{id}` | Get job detail | Bearer |
| POST | `/api/v1/jobs` | Create job posting | EMPLOYER |
| PUT | `/api/v1/jobs/{id}` | Update own job | EMPLOYER |
| DELETE | `/api/v1/jobs/{id}` | Close own job | EMPLOYER |
| GET | `/api/v1/jobs/my` | List employer's own jobs | EMPLOYER |

### Swipes
| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/api/v1/swipes/jobs/{jobId}` | Candidate swipes a job (LIKE/PASS) | USER |
| POST | `/api/v1/swipes/applicants/{applicationId}` | Employer swipes an applicant (LIKE/REJECT) | EMPLOYER |
| GET | `/api/v1/swipes/jobs/{jobId}/likes` | See who liked your job (PREMIUM) | EMPLOYER + PREMIUM |

### Applications
| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/api/v1/applications/jobs/{jobId}` | Apply to a job | USER |
| GET | `/api/v1/applications/my` | Candidate: own applications | USER |
| GET | `/api/v1/applications/jobs/{jobId}` | Employer: applicants for job | EMPLOYER |
| PUT | `/api/v1/applications/{id}/status` | Update application status | EMPLOYER |

### Matches
| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/v1/matches` | List own matches | Bearer |
| GET | `/api/v1/matches/{id}` | Match detail | Bearer |

### Notifications
| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/v1/notifications` | List notifications (paginated) | Bearer |
| PUT | `/api/v1/notifications/{id}/read` | Mark as read | Bearer |
| PUT | `/api/v1/notifications/read-all` | Mark all as read | Bearer |

### Subscriptions
| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/v1/subscriptions/plans` | List available plans | Public |
| POST | `/api/v1/subscriptions/checkout` | Create Stripe checkout session | Bearer |
| POST | `/api/v1/subscriptions/webhook` | Stripe webhook receiver | Stripe-Signature |
| GET | `/api/v1/subscriptions/me` | Current subscription status | Bearer |

### Admin
| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/v1/admin/users` | List all users (paginated) | ADMIN |
| PUT | `/api/v1/admin/users/{id}/ban` | Ban a user | ADMIN |
| PUT | `/api/v1/admin/users/{id}/role` | Change user role | ADMIN |
| DELETE | `/api/v1/admin/jobs/{id}` | Force-remove a job | ADMIN |

---

## 5. Matching Algorithm

### Three-Condition Gate

```
MATCH = (candidate_swiped_LIKE on job)
     AND (candidate_submitted_application for job)
     AND (employer_swiped_LIKE on applicant)
```

### Event-Driven Evaluation

The match gate is evaluated reactively by the **Match Service** on every relevant event:

```
Event: SwipeEvent (EMPLOYER → LIKE)
  → Query: does candidate_swipes record exist with action=LIKE for this job?
  → Query: does application record exist for (candidateId, jobId)?
  → If both true → create Match record → publish MatchEvent

Event: ApplicationEvent (candidate submits)
  → Query: did candidate swipe LIKE on this job?
  → Query: did employer already swipe LIKE on this candidate? (check employer_swipes)
  → If both true → create Match (rare ordering, but handled)

Event: SwipeEvent (CANDIDATE → LIKE)
  → No match evaluation yet (application not yet submitted)
  → Stored as pending signal only
```

### Idempotency
- Match creation uses a UNIQUE constraint on `(candidate_id, job_id)` in the `matches` table
- Kafka consumer uses exactly-once semantics via idempotent producer + consumer group offsets committed after processing
- DB upsert with `INSERT ... ON CONFLICT DO NOTHING` prevents duplicate matches

### Feed Algorithm (Candidate Job Feed)
1. Exclude jobs the candidate already swiped (LIKE or PASS)
2. Exclude jobs from employers who banned the candidate
3. Boost premium employer jobs to the top
4. Rank remaining by: relevance score (skills match) → recency → salary fit
5. Return paginated cursor (keyset pagination on `(boost_score DESC, posted_at DESC, id DESC)`)

---

## 6. Premium Logic

### Feature Gates

| Feature | Free | Premium |
|---------|------|---------|
| Daily swipes | 20/day | Unlimited |
| See who liked your profile/job | No | Yes |
| Boosted visibility in feed | No | Yes |
| Advanced filters (salary, company size) | Basic | Full |
| Application read receipts | No | Yes |

### Implementation

- `SubscriptionService.isActive(userId): boolean` is the single source of truth
- Rate limiting for free users: Redis key `swipe:limit:{userId}:{date}` with TTL to midnight
- Boost score: `jobs` table column `boost_score INT DEFAULT 0`; premium employer jobs get `boost_score = 100`; reset nightly via scheduled job

```java
// Centralized entitlement check (never inline this logic in controllers)
public void assertCanSwipe(Long userId) {
    if (subscriptionService.isActive(userId)) return;
    long todayCount = redisSwipeCounter.get(userId);
    if (todayCount >= FREE_SWIPE_LIMIT) {
        throw new SwipeLimitExceededException("Upgrade to premium for unlimited swipes");
    }
}
```

---

## 7. Authentication & Authorization

### JWT Flow
```
Login → issue accessToken (15m) + refreshToken (7d, stored in Redis)
Request → Bearer accessToken in Authorization header
Token expired → POST /auth/refresh with refreshToken cookie
Logout → access token added to Redis deny-list (TTL = remaining access token lifetime)
```

### Spring Security Configuration
- `SecurityFilterChain` with stateless session
- Custom `JwtAuthenticationFilter` extends `OncePerRequestFilter`
- Method-level security via `@PreAuthorize("hasRole('EMPLOYER')")` on service layer
- CORS configured for known frontend origins only

### Role Hierarchy
```
ADMIN > EMPLOYER = USER
PREMIUM is an additive role granted alongside USER or EMPLOYER
```

### Security Headers
- HSTS, X-Frame-Options: DENY, X-Content-Type-Options: nosniff
- CSRF disabled (stateless JWT API)
- Rate limiting at gateway level (Kong plugins or Spring filter + Redis)

---

## 8. Kafka Events and Flows

### Topics

| Topic | Partitions | Retention | Description |
|-------|-----------|-----------|-------------|
| `swipe.events` | 12 | 7 days | All swipe actions (candidate + employer) |
| `application.events` | 12 | 7 days | Application submissions and status changes |
| `match.events` | 6 | 30 days | Match creation events |
| `notification.events` | 6 | 3 days | Notification dispatch requests |

### Event Schemas (Avro / JSON)

```json
// SwipeEvent
{
  "eventId": "uuid",
  "actorId": "userId",
  "actorRole": "CANDIDATE | EMPLOYER",
  "targetId": "jobId | applicationId",
  "action": "LIKE | PASS | REJECT",
  "timestamp": "ISO-8601"
}

// ApplicationEvent
{
  "eventId": "uuid",
  "candidateId": "userId",
  "jobId": "uuid",
  "status": "SUBMITTED | REVIEWED | ACCEPTED | REJECTED",
  "timestamp": "ISO-8601"
}

// MatchEvent
{
  "eventId": "uuid",
  "matchId": "uuid",
  "candidateId": "userId",
  "employerId": "userId",
  "jobId": "uuid",
  "matchedAt": "ISO-8601"
}
```

### Consumer Groups

| Consumer Group | Topics Consumed | Action |
|---------------|----------------|--------|
| `match-service` | `swipe.events`, `application.events` | Evaluate match gate |
| `notification-service` | `match.events`, `application.events` | Create + dispatch notifications |
| `analytics-service` | All topics | Write to data warehouse |

---

## 9. Redis Caching Strategy

### Cache Keys and TTLs

| Key Pattern | Value | TTL | Invalidation Trigger |
|-------------|-------|-----|----------------------|
| `job:{jobId}` | Serialized Job JSON | 10 min | Job update/delete |
| `job:feed:{userId}:{page}` | Paginated job list | 2 min | New job posted or job swiped |
| `user:{userId}` | Serialized User JSON | 15 min | Profile update |
| `swipe:limit:{userId}:{date}` | Integer counter | Until midnight | Swipe action (INCR) |
| `token:deny:{jti}` | `1` | Remaining token TTL | Logout |
| `refresh:{userId}` | Refresh token hash | 7 days | Login / refresh / logout |
| `boost:jobs` | Sorted set (jobId → score) | 1 hour | Premium subscription change |

### Cache-Aside Pattern
All reads check Redis first, fall through to PostgreSQL on miss, then populate Redis. Write-through for profile updates. Write-invalidate (delete key) for job postings.

### Rate Limiting Pattern (Token Bucket via Redis)
```
MULTI
  INCR swipe:limit:{userId}:{date}
  EXPIREAT swipe:limit:{userId}:{date} {midnight_unix}
EXEC
→ if result[0] > FREE_SWIPE_LIMIT → reject with 429
```

---

## 10. Deployment Strategy

### Kubernetes Workloads

```yaml
# Core API: 3 replicas, HPA 3–10 based on CPU/RPS
# Match Service: 2 replicas (Kafka consumer, stateless)
# Notification Service: 2 replicas (Kafka consumer)
# All services: resource requests + limits defined
# PodDisruptionBudget: minAvailable: 1 for all deployments
```

### Environments
- **dev**: Single-node k3s, localstack for AWS, Kafka in Docker Compose
- **staging**: EKS (2 nodes), RDS t3.medium, shared ElastiCache
- **production**: EKS (multi-AZ, 3+ nodes), RDS Multi-AZ r6g.large, dedicated ElastiCache cluster, MSK Kafka

### CI/CD Pipeline (GitHub Actions)
```
Push → unit tests → integration tests (Testcontainers) → build Docker image
→ push to ECR → deploy to staging via Helm → smoke tests → manual gate → deploy prod
```

### Blue-Green Deployment
- Use Kubernetes `Deployment` with rolling update strategy (maxSurge: 1, maxUnavailable: 0)
- Database migrations run as Kubernetes `Job` before pod rollout (Flyway)

### Database Migrations
- Flyway for versioned migrations
- All migrations must be backward-compatible (add columns nullable first, migrate data, then add constraints)
- Never drop columns in the same migration that removes application references

---

## 11. Security Considerations

### Authentication
- Passwords: BCrypt, cost factor 12 (never store plaintext)
- JWT signed with RS256 (asymmetric keys); public key exposed at `/auth/.well-known/jwks.json`
- Refresh tokens stored as SHA-256 hash in Redis (never store raw token)
- MFA via TOTP planned for Phase 2

### Authorization
- All employer-facing endpoints verify job ownership before any read/write
- Candidates cannot see other candidates' applications
- Employer cannot see candidate profiles unless the candidate has applied to their job
- Admin endpoints isolated behind separate role check

### Input Validation
- All DTOs validated with Jakarta Bean Validation (`@NotBlank`, `@Size`, `@Pattern`)
- SQL injection: mitigated by JPA/JPQL parameterized queries (never string concatenation)
- XSS: sanitize all rich-text fields with OWASP Java HTML Sanitizer before storage
- File uploads: validate MIME type server-side, scan with ClamAV (async), store only in S3 (never local filesystem)

### API Security
- Rate limiting: 100 req/min per IP (unauthenticated), 500 req/min per user (authenticated)
- All endpoints over HTTPS only (enforce via HSTS)
- Sensitive fields (salary expectations) masked in logs
- PII fields encrypted at rest using AES-256 column encryption for SSN/DOB if collected

### Secrets Management
- AWS Secrets Manager for DB credentials, JWT keys, Stripe keys
- No secrets in environment variables in production (mounted as K8s secrets from Secrets Manager)

---

## 12. Observability Plan

### Metrics (Prometheus + Grafana)
- Spring Boot Actuator exports `/actuator/prometheus`
- Custom metrics: `swipes_total{action="LIKE|PASS"}`, `matches_created_total`, `applications_submitted_total`
- JVM metrics: heap, GC pause, thread count
- SLO dashboards: p50/p95/p99 latency, error rate, availability

### Logging (ELK / CloudWatch)
- Structured JSON logs (Logback + logstash-logback-encoder)
- Correlation ID (UUID) injected in MDC at filter level, propagated through Kafka headers
- Log levels: ERROR for unhandled exceptions, WARN for business rule violations, INFO for state changes, DEBUG off in prod

### Tracing (OpenTelemetry + AWS X-Ray)
- Trace spans for: HTTP request, DB query, Redis call, Kafka publish/consume
- Trace propagation via W3C `traceparent` header

### Alerting
- PagerDuty alerts for: error rate > 1%, p99 latency > 2s, Kafka consumer lag > 10k, DB connection pool exhaustion

### Health Checks
- `/actuator/health` with readiness and liveness probes
- Kubernetes liveness: `/actuator/health/liveness` (JVM alive)
- Kubernetes readiness: `/actuator/health/readiness` (DB + Redis reachable)

---

## 13. Error Handling & Edge Cases

### Global Exception Handler
`@RestControllerAdvice` with `@ExceptionHandler` mappings:

| Exception | HTTP Status | Response Body |
|-----------|-------------|---------------|
| `ResourceNotFoundException` | 404 | `{ error, message, path }` |
| `AccessDeniedException` | 403 | `{ error, message }` |
| `ValidationException` | 400 | `{ error, violations: [...] }` |
| `SwipeLimitExceededException` | 429 | `{ error, upgradeUrl }` |
| `DuplicateApplicationException` | 409 | `{ error, message }` |
| `RuntimeException` (unhandled) | 500 | `{ error, traceId }` (no stack trace) |

### Business Edge Cases

| Scenario | Handling |
|----------|----------|
| Candidate applies without swiping LIKE | Reject with 422: must swipe LIKE first |
| Employer swipes on non-applicant | Reject with 403: can only swipe on own applicants |
| Job closed after candidate swiped LIKE | Application blocked with 410: Job no longer accepting applications |
| Duplicate swipe (swipe same job twice) | Idempotent: update existing record, do not create duplicate |
| Duplicate application | Reject with 409: already applied |
| Match re-evaluated after conditions already met | `INSERT ... ON CONFLICT DO NOTHING` — safe no-op |
| Premium expires mid-session | Swipe limit enforced on next request (Redis counter checked fresh each time) |
| Kafka consumer failure | Dead letter topic `{topic}.DLT`; alert fires; manual replay after fix |
| S3 upload failure | Return pre-signed URL failure to client; photo not updated; no partial state |

---

## 14. Project Module Structure

```
job-cupid/
├── src/main/java/com/jobcupid/
│   ├── auth/
│   │   ├── controller/AuthController.java
│   │   ├── service/AuthService.java
│   │   ├── service/TokenService.java
│   │   └── dto/
│   ├── user/
│   │   ├── controller/UserController.java
│   │   ├── service/UserService.java
│   │   ├── domain/User.java
│   │   └── repository/UserRepository.java
│   ├── job/
│   │   ├── controller/JobController.java
│   │   ├── service/JobService.java
│   │   ├── domain/Job.java
│   │   └── repository/JobRepository.java
│   ├── swipe/
│   │   ├── controller/SwipeController.java
│   │   ├── service/SwipeService.java
│   │   ├── domain/CandidateSwipe.java
│   │   ├── domain/EmployerSwipe.java
│   │   └── event/SwipeEventPublisher.java
│   ├── application/
│   │   ├── controller/ApplicationController.java
│   │   ├── service/ApplicationService.java
│   │   ├── domain/Application.java
│   │   └── event/ApplicationEventPublisher.java
│   ├── match/
│   │   ├── consumer/SwipeEventConsumer.java
│   │   ├── consumer/ApplicationEventConsumer.java
│   │   ├── service/MatchService.java
│   │   ├── domain/Match.java
│   │   └── event/MatchEventPublisher.java
│   ├── notification/
│   │   ├── consumer/MatchEventConsumer.java
│   │   ├── service/NotificationService.java
│   │   └── domain/Notification.java
│   ├── subscription/
│   │   ├── controller/SubscriptionController.java
│   │   ├── service/SubscriptionService.java
│   │   └── domain/Subscription.java
│   ├── admin/
│   │   └── controller/AdminController.java
│   └── shared/
│       ├── exception/
│       ├── config/
│       ├── security/
│       └── util/
├── src/test/java/com/jobcupid/
│   └── (mirrors main structure)
├── src/main/resources/
│   ├── db/migration/          # Flyway scripts
│   ├── application.yml
│   ├── application-dev.yml
│   └── application-prod.yml
├── k8s/
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── hpa.yaml
│   └── ingress.yaml
├── docker-compose.yml         # Local dev: PG + Redis + Kafka
├── Dockerfile
└── pom.xml
```
