# Job-Cupid: Task Plan

> **Implementation order is deliberate.** Each epic depends on the ones before it.
> Complete epics sequentially. Within an epic, tasks must also be done in order unless noted as independent.
>
> **TDD rule:** Write the test first. Make it fail. Then write the implementation. Then make it pass.

---

## Epic 1 — Project Foundation

Fix all broken stubs and establish the infrastructure every other epic depends on: config, Flyway, error handling, and pom additions.

---

### Task 1.1 — Fix Existing Stubs

**Description**
All generated stubs have compilation errors or missing functionality. Fix them before writing any new code so the project compiles clean from the start.

**Files to fix:**
- `User.java` — add missing `package` declaration; add missing `@EnumType` import
- `UserRole.java` — add `PREMIUM` value
- `AuthRepsonse.java` — rename to `AuthResponse.java`; replace fields with `accessToken`, `refreshToken`, `tokenType`, `userId`, `email`, `role`, `isPremium`; add Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor`; remove `passwordHash`
- `UserRepository.java` — extend `JpaRepository<User, UUID>`; add `findByEmail(String email): Optional<User>`; add `existsByEmail(String email): boolean`
- `UserController.java` — rename class to `UserController`; add `package` declaration; leave body empty for Task 3.x

**Acceptance Criteria**
- `mvn compile` passes with zero errors
- `UserRole.PREMIUM` is reachable from application code
- `AuthResponse` has no `passwordHash` field
- `UserRepository` extends `JpaRepository<User, UUID>`

**Test Cases (TDD)**
```
UserRoleTest:
  - GIVEN UserRole enum WHEN values() called THEN contains USER, EMPLOYER, ADMIN, PREMIUM

UserRepositoryTest (slice: @DataJpaTest):
  - GIVEN a saved User WHEN findByEmail(email) THEN returns the user
  - GIVEN no user with email WHEN findByEmail(unknown) THEN returns Optional.empty()
  - GIVEN a saved User WHEN existsByEmail(email) THEN returns true
  - GIVEN no user WHEN existsByEmail(unknown) THEN returns false
```

**APIs Involved:** None

**DB Changes:** None (entity already maps to schema)

**Complexity:** S

---

### Task 1.2 — Full Application Configuration

**Description**
`application.properties` has only the app name. Create `application.yml` (replace properties) with full configuration split across three profiles: `default` (shared), `dev`, `prod`.

**Configuration sections to add:**
- Server: port 8080, servlet context path `/`
- DataSource: PostgreSQL JDBC URL, username, password, Hikari pool (min 5, max 20)
- JPA: `ddl-auto=validate`, show-sql off in prod, dialect `PostgreSQLDialect`
- Flyway: enabled, locations `classpath:db/migration`, baseline-on-migrate true
- Redis: host, port, password, lettuce pool
- Kafka: bootstrap-servers, consumer group `job-cupid-core`, auto-offset-reset earliest
- JWT: secret key placeholder, access-token TTL 900s, refresh-token TTL 604800s
- AWS: region, S3 bucket name
- Actuator: expose health, info, prometheus endpoints

**Acceptance Criteria**
- Application starts with `spring.profiles.active=dev` using a local Postgres + Redis
- `application-dev.yml` uses `localhost` datasource; `application-prod.yml` reads from env vars
- No secrets committed — all prod credentials use `${ENV_VAR}` placeholders
- `/actuator/health` returns `UP`

**Test Cases (TDD)**
```
ApplicationConfigTest (@SpringBootTest, profile=dev, @TestPropertySource):
  - GIVEN dev profile WHEN context loads THEN DataSource bean is not null
  - GIVEN dev profile WHEN context loads THEN KafkaTemplate bean is not null
  - GIVEN dev profile WHEN context loads THEN RedisTemplate bean is not null
```

**APIs Involved:** `GET /actuator/health`

**DB Changes:** None

**Complexity:** S

---

### Task 1.3 — Docker Compose for Local Development

**Description**
Create `docker-compose.yml` at project root to spin up all local infrastructure dependencies: PostgreSQL 16, Redis 7, Apache Kafka (with Zookeeper), and Kafka UI for debugging.

**Services:**
- `postgres`: image `postgres:16`, port 5432, volume for data persistence
- `redis`: image `redis:7-alpine`, port 6379
- `zookeeper`: image `confluentinc/cp-zookeeper:7.6`
- `kafka`: image `confluentinc/cp-kafka:7.6`, port 9092, depends on zookeeper; create topics `swipe.events`, `application.events`, `match.events`, `notification.events` on startup
- `kafka-ui`: image `provectuslabs/kafka-ui`, port 8090

**Acceptance Criteria**
- `docker-compose up -d` starts all services with no errors
- Application connects to Postgres and Redis when started with `dev` profile
- Kafka UI at `http://localhost:8090` shows all four topics created

**Test Cases (TDD)**
```
(Infrastructure — verified manually; no unit tests for docker-compose)
IntegrationBaseTest:
  - GIVEN docker-compose up WHEN application starts THEN contextLoads() passes (existing test now passes with full config)
```

**APIs Involved:** None

**DB Changes:** None

**Complexity:** S

---

### Task 1.4 — Flyway Migrations: Core Tables

**Description**
Create Flyway migration scripts for all tables defined in `database_schema.md`. Scripts run in order on startup. Each script must be idempotent and backward-compatible.

**Migration files to create:**
```
V1__create_users.sql
V2__create_candidate_employer_profiles.sql
V3__create_jobs_and_trigger.sql
V4__create_swipes.sql
V5__create_applications.sql
V6__create_matches.sql
V7__create_subscriptions.sql
V8__create_notifications.sql
V9__create_refresh_tokens.sql
```

Each file contains the exact DDL from `database_schema.md` including constraints and indexes.

**Acceptance Criteria**
- `mvn flyway:migrate` completes with all 9 scripts applied successfully
- `flyway_schema_history` table shows all migrations with `success = true`
- All unique constraints, foreign keys, and indexes exist (verify with `\d` in psql)
- Re-running migrations is a no-op (idempotent)

**Test Cases (TDD)**
```
FlywayMigrationTest (@SpringBootTest, @Testcontainers):
  - GIVEN a clean Postgres container WHEN app starts THEN flyway_schema_history has 9 rows all success=true
  - GIVEN migrations applied WHEN checking schema THEN users table has columns: id, email, password_hash, role, is_premium, deleted_at
  - GIVEN migrations applied WHEN checking schema THEN matches has UNIQUE constraint on (candidate_id, job_id)
```

**APIs Involved:** None

**DB Changes:** All 9 tables created (full schema)

**Complexity:** M

---

### Task 1.5 — Global Exception Handler & Error Response DTO

**Description**
Create a `@RestControllerAdvice` global exception handler so every error returns a consistent JSON structure. Create all custom exception classes that the application will throw.

**Files to create:**
- `shared/exception/ResourceNotFoundException.java` (extends RuntimeException)
- `shared/exception/AccessDeniedException.java`
- `shared/exception/ValidationException.java`
- `shared/exception/SwipeLimitExceededException.java`
- `shared/exception/DuplicateApplicationException.java`
- `shared/exception/BusinessRuleException.java` (generic 422)
- `shared/dto/ErrorResponse.java` — fields: `timestamp`, `status`, `error`, `message`, `path`, `traceId`
- `shared/handler/GlobalExceptionHandler.java` — `@RestControllerAdvice` with `@ExceptionHandler` for each exception + `MethodArgumentNotValidException` (400) + generic `Exception` (500, no stack trace)

**Acceptance Criteria**
- All 4xx errors return `ErrorResponse` JSON with correct HTTP status
- 500 errors return `ErrorResponse` with `traceId` but no stack trace in the body
- `MethodArgumentNotValidException` returns 400 with a `violations` list
- `SwipeLimitExceededException` returns 429 with an `upgradeUrl` field

**Test Cases (TDD)**
```
GlobalExceptionHandlerTest (@WebMvcTest):
  - GIVEN controller throws ResourceNotFoundException WHEN request made THEN 404 + ErrorResponse body
  - GIVEN controller throws SwipeLimitExceededException WHEN request made THEN 429 + upgradeUrl in body
  - GIVEN invalid request body WHEN POST with missing required field THEN 400 + violations list not empty
  - GIVEN controller throws RuntimeException WHEN request made THEN 500 + traceId present + no stackTrace field
```

**APIs Involved:** Applies to all endpoints

**DB Changes:** None

**Complexity:** S

---

### Task 1.6 — Add Missing Dependencies to pom.xml

**Description**
The existing `pom.xml` is missing libraries needed for JWT, AWS S3, Stripe, and OpenTelemetry. Add them now so they are available for later epics.

**Dependencies to add:**
- `io.jsonwebtoken:jjwt-api:0.12.6` + `jjwt-impl` + `jjwt-jackson` (runtime)
- `software.amazon.awssdk:s3:2.25.x` (AWS SDK v2)
- `com.stripe:stripe-java:25.x`
- `io.micrometer:micrometer-registry-prometheus`
- `io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter:2.x`
- `org.testcontainers:postgresql` + `kafka` + `junit-jupiter` (test scope)
- `org.flywaydb:flyway-core` (if not auto-included by Spring Boot)

**Acceptance Criteria**
- `mvn dependency:resolve` succeeds with no missing artifacts
- `mvn compile` still passes
- No dependency version conflicts (`mvn dependency:tree` is clean)

**Test Cases (TDD)**
```
(No new tests — existing tests must still pass: mvn test)
```

**APIs Involved:** None

**DB Changes:** None

**Complexity:** S

---

## Epic 2 — Authentication

Build the full stateless JWT authentication system. This is a hard prerequisite for every subsequent epic.

---

### Task 2.1 — JWT Token Service

**Description**
Create `TokenService` that issues, validates, and revokes JWTs. Uses RS256 (asymmetric keys). Access token carries `userId`, `email`, `role`, `isPremium` claims.

**Files to create:**
- `auth/service/TokenService.java`
  - `generateAccessToken(User user): String` — signed JWT, 15 min TTL
  - `generateRefreshToken(User user): String` — opaque UUID stored in Redis
  - `validateAccessToken(String token): Claims`
  - `blacklistAccessToken(String jti, long remainingMillis)` — write to Redis deny-list
  - `isAccessTokenBlacklisted(String jti): boolean`
  - `storeRefreshToken(UUID userId, String tokenHash, Duration ttl)`
  - `rotateRefreshToken(UUID userId, String oldHash, String newHash)`
  - `revokeRefreshToken(UUID userId)`
- `auth/config/JwtProperties.java` — `@ConfigurationProperties("jwt")` binding secret, TTLs
- `shared/config/KeyPairConfig.java` — generate or load RSA KeyPair from config

**Acceptance Criteria**
- Access token validates successfully within TTL
- Expired access token throws `ExpiredJwtException`
- Tampered token throws `JwtException`
- Blacklisted token is detected by `isAccessTokenBlacklisted()`
- Refresh token rotation revokes old hash and stores new one in Redis

**Test Cases (TDD)**
```
TokenServiceTest (@ExtendWith(MockitoExtension.class)):
  - GIVEN a User WHEN generateAccessToken() THEN token contains userId, email, role claims
  - GIVEN a valid token WHEN validateAccessToken() THEN returns Claims without exception
  - GIVEN an expired token WHEN validateAccessToken() THEN throws ExpiredJwtException
  - GIVEN a tampered token WHEN validateAccessToken() THEN throws JwtException
  - GIVEN a blacklisted jti WHEN isAccessTokenBlacklisted() THEN returns true
  - GIVEN a non-blacklisted jti WHEN isAccessTokenBlacklisted() THEN returns false

TokenServiceIntegrationTest (@SpringBootTest, Testcontainers Redis):
  - GIVEN refresh token stored WHEN rotateRefreshToken() called THEN old hash removed, new hash present
  - GIVEN revokeRefreshToken() called WHEN checking old hash THEN returns false
```

**APIs Involved:** None (internal service)

**DB Changes:** None (uses Redis only for token state)

**Complexity:** M

---

### Task 2.2 — Spring Security Configuration

**Description**
Configure `SecurityFilterChain` for stateless JWT authentication. Define which endpoints are public vs protected. Set up role-based access rules.

**Files to create:**
- `shared/security/SecurityConfig.java` — `@Configuration @EnableMethodSecurity`
  - Disable CSRF (stateless API)
  - Disable session creation (`STATELESS`)
  - Public: `POST /auth/**`, `GET /api/v1/jobs` (browse), `GET /api/v1/subscriptions/plans`, `POST /api/v1/subscriptions/webhook`
  - Protected: everything else requires `authenticated()`
  - CORS: allow configured frontend origin only
  - Security headers: HSTS, X-Frame-Options DENY, X-Content-Type-Options nosniff
- `shared/security/JwtAuthenticationFilter.java` — `OncePerRequestFilter`
  - Extract Bearer token from `Authorization` header
  - Validate via `TokenService`
  - Check deny-list via `TokenService.isAccessTokenBlacklisted()`
  - Set `UsernamePasswordAuthenticationToken` in `SecurityContextHolder`
- `shared/security/UserPrincipal.java` — implements `UserDetails`, wraps `User` entity claims

**Acceptance Criteria**
- Request without token to protected endpoint returns 401
- Request with valid token sets correct `UserPrincipal` in context
- Request with blacklisted token returns 401
- Request with expired token returns 401
- `POST /auth/login` is accessible without a token
- `@PreAuthorize("hasRole('EMPLOYER')")` correctly rejects USER-role requests

**Test Cases (TDD)**
```
JwtAuthenticationFilterTest (@WebMvcTest with SecurityConfig):
  - GIVEN no Authorization header WHEN GET /api/v1/users/me THEN 401
  - GIVEN valid Bearer token WHEN GET /api/v1/users/me THEN 200 (with mock controller)
  - GIVEN expired Bearer token WHEN GET /api/v1/users/me THEN 401
  - GIVEN blacklisted jti WHEN GET /api/v1/users/me THEN 401

SecurityConfigTest:
  - GIVEN no token WHEN POST /auth/login THEN not blocked by security (reaches controller)
  - GIVEN USER role token WHEN endpoint requires EMPLOYER THEN 403
```

**APIs Involved:** Applies to all endpoints

**DB Changes:** None

**Complexity:** M

---

### Task 2.3 — Register Endpoint

**Description**
Allow new candidates and employers to register. Validate input, hash password, persist user, return JWT pair.

**Files to create/update:**
- `auth/dto/RegisterRequest.java` — `email`, `password`, `firstName`, `lastName`, `role` (USER or EMPLOYER only — ADMIN not self-registerable)
- `auth/service/AuthService.java` — `register(RegisterRequest): AuthResponse`
  - Check `userRepository.existsByEmail()` → throw `DuplicateEmailException` (409) if taken
  - Hash password with BCrypt (cost 12)
  - Save `User` entity
  - Generate access + refresh tokens
  - Return `AuthResponse`
- `auth/controller/AuthController.java` — `POST /auth/register`

**Acceptance Criteria**
- Valid registration returns 201 with `AuthResponse` containing `accessToken` and `refreshToken`
- Registering with existing email returns 409
- Registering with `role=ADMIN` returns 400
- Password is never returned in any response
- BCrypt hash stored (never plaintext)

**Test Cases (TDD)**
```
AuthServiceTest (Mockito):
  - GIVEN valid RegisterRequest WHEN register() THEN user saved with BCrypt hash, returns AuthResponse
  - GIVEN duplicate email WHEN register() THEN throws DuplicateEmailException
  - GIVEN role=ADMIN in request WHEN register() THEN throws ValidationException

AuthControllerTest (@WebMvcTest):
  - GIVEN valid JSON body WHEN POST /auth/register THEN 201 + accessToken in body
  - GIVEN missing email field WHEN POST /auth/register THEN 400 + violations
  - GIVEN duplicate email WHEN POST /auth/register THEN 409
  - GIVEN weak password (< 8 chars) WHEN POST /auth/register THEN 400

AuthIntegrationTest (@SpringBootTest, Testcontainers):
  - GIVEN valid registration WHEN POST /auth/register THEN user row exists in DB with non-null password_hash
  - GIVEN registration WHEN inspecting DB THEN password_hash starts with "$2a$" (BCrypt)
```

**APIs Involved:** `POST /auth/register`

**DB Changes:** Inserts into `users` table

**Complexity:** M

---

### Task 2.4 — Login Endpoint

**Description**
Authenticate user with email + password. Return JWT access and refresh tokens. Update `last_login_at`.

**Files to create/update:**
- `auth/dto/LoginRequest.java` — `email`, `password`
- `AuthService.login(LoginRequest): AuthResponse`
  - Load user by email; throw `InvalidCredentialsException` (401) if not found
  - `BCryptPasswordEncoder.matches()` to verify — throw 401 if mismatch
  - Check `isActive` and `isBanned`; throw 403 if banned
  - Generate tokens, update `lastLoginAt`, return `AuthResponse`
- `AuthController` — `POST /auth/login`

**Acceptance Criteria**
- Valid credentials return 200 with `accessToken` + `refreshToken`
- Wrong password returns 401 (same message as wrong email — no user enumeration)
- Banned user returns 403
- `last_login_at` updated in DB on successful login

**Test Cases (TDD)**
```
AuthServiceTest:
  - GIVEN correct credentials WHEN login() THEN returns AuthResponse with non-null tokens
  - GIVEN wrong password WHEN login() THEN throws InvalidCredentialsException
  - GIVEN unknown email WHEN login() THEN throws InvalidCredentialsException (same exception — no enumeration)
  - GIVEN banned user WHEN login() THEN throws UserBannedException

AuthControllerTest:
  - GIVEN valid credentials WHEN POST /auth/login THEN 200 + accessToken
  - GIVEN wrong password WHEN POST /auth/login THEN 401
  - GIVEN missing password field WHEN POST /auth/login THEN 400

AuthIntegrationTest:
  - GIVEN login succeeds WHEN querying DB THEN last_login_at is not null and recent
```

**APIs Involved:** `POST /auth/login`

**DB Changes:** Updates `users.last_login_at`

**Complexity:** S

---

### Task 2.5 — Refresh Token Endpoint

**Description**
Rotate refresh tokens. Client sends refresh token (in `Authorization: Bearer` or cookie), gets a new access + refresh token pair. Old refresh token is invalidated.

**Files to create/update:**
- `auth/dto/RefreshRequest.java` — `refreshToken: String`
- `AuthService.refresh(String refreshToken): AuthResponse`
  - Hash the raw token; look up in Redis
  - If not found or expired → throw `InvalidTokenException` (401)
  - Generate new access + refresh token pair
  - Rotate: delete old hash, store new hash in Redis
  - Return `AuthResponse`
- `AuthController` — `POST /auth/refresh`

**Acceptance Criteria**
- Valid refresh token returns 200 with new token pair
- Old refresh token is invalidated after rotation (cannot be reused)
- Invalid/expired/unknown refresh token returns 401
- Rotated tokens belong to the same user

**Test Cases (TDD)**
```
AuthServiceTest:
  - GIVEN valid refresh token WHEN refresh() THEN returns new AuthResponse, old token invalidated
  - GIVEN unknown token WHEN refresh() THEN throws InvalidTokenException
  - GIVEN already-rotated token WHEN refresh() second time THEN throws InvalidTokenException (replay protection)

AuthControllerTest:
  - GIVEN valid refresh token WHEN POST /auth/refresh THEN 200 + new accessToken
  - GIVEN invalid token WHEN POST /auth/refresh THEN 401
```

**APIs Involved:** `POST /auth/refresh`

**DB Changes:** None (Redis only)

**Complexity:** S

---

### Task 2.6 — Logout Endpoint

**Description**
Invalidate the current session. Blacklist the access token JTI in Redis. Revoke the refresh token.

**Files to create/update:**
- `auth/dto/LogoutRequest.java` — `refreshToken: String`
- `AuthService.logout(String accessTokenJti, long remainingTtlMs, String refreshToken)`
  - Add JTI to Redis deny-list with TTL = remaining access token lifetime
  - Hash the refresh token and delete from Redis
- `AuthController` — `POST /auth/logout` (requires Bearer token)

**Acceptance Criteria**
- After logout, the same access token returns 401 on any protected endpoint
- After logout, the same refresh token cannot be used to refresh
- Returns 200 even if refresh token was already expired (idempotent)

**Test Cases (TDD)**
```
AuthServiceTest:
  - GIVEN active session WHEN logout() THEN JTI added to deny-list, refresh token revoked
  - GIVEN already-logged-out token WHEN logout() called again THEN no exception (idempotent)

AuthIntegrationTest:
  - GIVEN logged-in user WHEN POST /auth/logout THEN subsequent GET /api/v1/users/me returns 401
  - GIVEN logged-in user WHEN POST /auth/logout THEN POST /auth/refresh with old token returns 401
```

**APIs Involved:** `POST /auth/logout`

**DB Changes:** None (Redis only)

**Complexity:** S

---

## Epic 3 — User Profiles

---

### Task 3.1 — CandidateProfile Entity & Repository

**Description**
Create `CandidateProfile` entity (one-to-one with `User`), its repository, and service methods for creating and updating candidate profile data.

**Files to create:**
- `user/domain/CandidateProfile.java` — JPA entity mapping `candidate_profiles` table; fields: `userId`, `headline`, `resumeUrl`, `skills` (String array), `yearsOfExperience`, `desiredSalaryMin`, `desiredSalaryMax`, `preferredRemote`, `preferredLocation`, `isOpenToWork`
- `user/repository/CandidateProfileRepository.java` — extends `JpaRepository<CandidateProfile, UUID>`; add `findByUserId(UUID userId): Optional<CandidateProfile>`
- `user/service/CandidateProfileService.java` — `getByUserId()`, `createOrUpdate(UUID userId, UpdateCandidateProfileRequest)`

**Acceptance Criteria**
- `CandidateProfile` is created automatically on first profile update (upsert)
- `findByUserId()` returns the profile when it exists
- Skills stored as PostgreSQL `text[]` array and retrievable as `List<String>`
- Salary range constraint validated: min <= max

**Test Cases (TDD)**
```
CandidateProfileRepositoryTest (@DataJpaTest):
  - GIVEN saved profile WHEN findByUserId() THEN returns profile
  - GIVEN no profile WHEN findByUserId() THEN returns Optional.empty()
  - GIVEN profile with skills WHEN saved THEN skills retrievable as list

CandidateProfileServiceTest (Mockito):
  - GIVEN no existing profile WHEN createOrUpdate() THEN new profile created
  - GIVEN existing profile WHEN createOrUpdate() THEN profile fields updated
  - GIVEN salaryMin > salaryMax WHEN createOrUpdate() THEN throws ValidationException
```

**APIs Involved:** `PUT /api/v1/users/me` (used in Task 3.3)

**DB Changes:** `candidate_profiles` table (created by Flyway V2)

**Complexity:** S

---

### Task 3.2 — EmployerProfile Entity & Repository

**Description**
Same pattern as Task 3.1 but for employers.

**Files to create:**
- `user/domain/EmployerProfile.java` — fields: `userId`, `companyName`, `companyDescription`, `companyWebsite`, `companyLogoUrl`, `companySize`, `industry`, `foundedYear`
- `user/repository/EmployerProfileRepository.java`
- `user/service/EmployerProfileService.java`

**Acceptance Criteria**
- Same as 3.1 pattern — upsert on update
- `companyName` is required (not null)
- `companySize` validated against allowed enum values

**Test Cases (TDD)**
```
EmployerProfileRepositoryTest (@DataJpaTest):
  - GIVEN saved employer profile WHEN findByUserId() THEN returns profile

EmployerProfileServiceTest:
  - GIVEN null companyName WHEN createOrUpdate() THEN throws ValidationException
  - GIVEN valid data WHEN createOrUpdate() THEN profile persisted
```

**APIs Involved:** `PUT /api/v1/users/me`

**DB Changes:** `employer_profiles` table (created by Flyway V2)

**Complexity:** S

---

### Task 3.3 — Get & Update Own Profile

**Description**
Implement `GET /api/v1/users/me` and `PUT /api/v1/users/me`. Returns different response shapes depending on role (candidate vs employer). Update delegates to appropriate profile service.

**Files to create:**
- `user/dto/CandidateProfileResponse.java`
- `user/dto/EmployerProfileResponse.java`
- `user/dto/UpdateCandidateProfileRequest.java` — all fields optional (PATCH semantics via PUT)
- `user/dto/UpdateEmployerProfileRequest.java`
- `user/controller/UserController.java` — implement `GET /api/v1/users/me` and `PUT /api/v1/users/me`
- `user/service/UserService.java` — `getCurrentUser(UUID userId)`, `updateProfile(UUID userId, request)`

**Acceptance Criteria**
- `GET /api/v1/users/me` returns correct shape based on role
- `PUT /api/v1/users/me` updates only provided fields (null fields unchanged)
- Cannot update `email`, `role`, or `isPremium` via this endpoint
- Returns 401 if not authenticated

**Test Cases (TDD)**
```
UserControllerTest (@WebMvcTest):
  - GIVEN authenticated USER WHEN GET /api/v1/users/me THEN 200 + CandidateProfileResponse shape
  - GIVEN authenticated EMPLOYER WHEN GET /api/v1/users/me THEN 200 + EmployerProfileResponse shape
  - GIVEN no auth WHEN GET /api/v1/users/me THEN 401
  - GIVEN UPDATE request with new headline WHEN PUT /api/v1/users/me THEN 200 + updated headline

UserServiceTest:
  - GIVEN userId WHEN getCurrentUser() THEN returns user + merged profile data
  - GIVEN partial update WHEN updateProfile() THEN only non-null fields changed
```

**APIs Involved:** `GET /api/v1/users/me`, `PUT /api/v1/users/me`

**DB Changes:** Updates `candidate_profiles` or `employer_profiles`

**Complexity:** M

---

### Task 3.4 — Soft Delete Account

**Description**
`DELETE /api/v1/users/me` sets `deleted_at = NOW()` and `is_active = false`. User cannot login after deletion.

**Files to create/update:**
- `UserService.softDeleteUser(UUID userId)` — set `deleted_at`, `isActive = false`
- `AuthService.login()` — check `deletedAt IS NULL` before authenticating
- `UserController` — `DELETE /api/v1/users/me`

**Acceptance Criteria**
- After deletion, `GET /api/v1/users/me` returns 404
- After deletion, login with same credentials returns 401
- User row still exists in DB with `deleted_at` set (not hard deleted)

**Test Cases (TDD)**
```
UserServiceTest:
  - GIVEN active user WHEN softDeleteUser() THEN deleted_at set, isActive=false

UserControllerTest:
  - GIVEN authenticated user WHEN DELETE /api/v1/users/me THEN 204

AuthServiceTest:
  - GIVEN soft-deleted user WHEN login() THEN throws InvalidCredentialsException
```

**APIs Involved:** `DELETE /api/v1/users/me`

**DB Changes:** Updates `users.deleted_at`, `users.is_active`

**Complexity:** S

---

### Task 3.5 — Profile Photo Upload (S3 Pre-Signed URL)

**Description**
Issue an S3 pre-signed PUT URL. Client uploads directly to S3. After upload, client calls confirm endpoint to save the URL to their profile.

**Files to create:**
- `shared/service/S3Service.java` — `generatePresignedPutUrl(String key, Duration expiry): URL`; `buildPublicUrl(String key): String`
- `user/dto/PhotoUploadResponse.java` — `presignedUrl`, `publicUrl`, `expiresIn`
- `UserController` — `POST /api/v1/users/me/photo` → returns `PhotoUploadResponse`; `PUT /api/v1/users/me/photo/confirm` → saves `publicUrl` to user profile

**Acceptance Criteria**
- `POST /api/v1/users/me/photo` returns a pre-signed URL valid for 5 minutes
- Pre-signed URL key is scoped to the authenticated user's ID (no path traversal)
- After confirm, `photo_url` in DB is updated
- File type restricted to `image/jpeg`, `image/png` (enforced via pre-signed URL content-type condition)

**Test Cases (TDD)**
```
S3ServiceTest (Mockito S3Presigner):
  - GIVEN userId and fileName WHEN generatePresignedPutUrl() THEN URL contains userId in key path
  - GIVEN key WHEN buildPublicUrl() THEN returns correct S3 public URL format

UserControllerTest:
  - GIVEN authenticated user WHEN POST /api/v1/users/me/photo THEN 200 + presignedUrl not null
  - GIVEN presignedUrl received WHEN PUT /api/v1/users/me/photo/confirm THEN 200 + photo_url updated
```

**APIs Involved:** `POST /api/v1/users/me/photo`, `PUT /api/v1/users/me/photo/confirm`

**DB Changes:** Updates `users.photo_url`

**Complexity:** M

---

## Epic 4 — Job Management

---

### Task 4.1 — Job Entity & Repository

**Description**
Create `Job` JPA entity, repository with required query methods, and the Flyway migration (already covered in Task 1.4 V3, so this task is entity + repo layer only).

**Files to create:**
- `job/domain/Job.java` — all fields from schema: `employerId`, `title`, `description`, `category`, `location`, `isRemote`, `salaryMin`, `salaryMax`, `currency`, `employmentType`, `experienceLevel`, `requiredSkills`, `status`, `boostScore`, `applicationCount`, `expiresAt`, `deletedAt`
- `job/domain/JobStatus.java` — enum: `ACTIVE`, `PAUSED`, `CLOSED`, `ARCHIVED`
- `job/domain/EmploymentType.java` — enum: `FULL_TIME`, `PART_TIME`, `CONTRACT`, `INTERNSHIP`, `FREELANCE`
- `job/repository/JobRepository.java` — extends `JpaRepository<Job, UUID>`
  - `findByIdAndDeletedAtIsNull(UUID id): Optional<Job>`
  - `findByEmployerIdAndDeletedAtIsNull(UUID employerId, Pageable pageable): Page<Job>`

**Acceptance Criteria**
- Job entity saves and retrieves all fields correctly
- Soft-deleted jobs not returned by `findByIdAndDeletedAtIsNull()`
- `requiredSkills` stored as PostgreSQL array and retrieved as `List<String>`

**Test Cases (TDD)**
```
JobRepositoryTest (@DataJpaTest):
  - GIVEN saved active job WHEN findByIdAndDeletedAtIsNull() THEN returns job
  - GIVEN soft-deleted job WHEN findByIdAndDeletedAtIsNull() THEN returns Optional.empty()
  - GIVEN employer with 3 jobs WHEN findByEmployerIdAndDeletedAtIsNull() THEN returns 3 jobs
```

**APIs Involved:** None (repository layer)

**DB Changes:** `jobs` table (created by Flyway V3)

**Complexity:** S

---

### Task 4.2 — Create & Update Job

**Description**
Employer creates and updates job postings. Only the owning employer can edit their own jobs.

**Files to create:**
- `job/dto/CreateJobRequest.java` — validated DTO: `title` required, `description` required, `category` required, `employmentType` required, salary range optional
- `job/dto/UpdateJobRequest.java` — all fields optional
- `job/dto/JobResponse.java`
- `job/service/JobService.java` — `createJob(UUID employerId, CreateJobRequest): JobResponse`; `updateJob(UUID employerId, UUID jobId, UpdateJobRequest): JobResponse` (checks ownership)
- `job/controller/JobController.java` — `POST /api/v1/jobs`, `PUT /api/v1/jobs/{id}`, `GET /api/v1/jobs/my`

**Acceptance Criteria**
- Employer can create a job returning 201 with `JobResponse`
- Non-employer role gets 403 on create
- Employer can only update their own jobs (403 if trying to edit another's)
- `status` defaults to `ACTIVE` on creation
- `boostScore` defaults to 0 (100 if employer is premium — set by `SubscriptionService`)

**Test Cases (TDD)**
```
JobServiceTest:
  - GIVEN EMPLOYER user WHEN createJob() THEN job saved with status=ACTIVE, boostScore=0
  - GIVEN premium EMPLOYER WHEN createJob() THEN job saved with boostScore=100
  - GIVEN EMPLOYER tries to update job owned by OTHER employer WHEN updateJob() THEN throws AccessDeniedException
  - GIVEN valid update WHEN updateJob() THEN only provided fields changed

JobControllerTest:
  - GIVEN USER role token WHEN POST /api/v1/jobs THEN 403
  - GIVEN EMPLOYER token + valid body WHEN POST /api/v1/jobs THEN 201 + jobId in response
  - GIVEN EMPLOYER token WHEN GET /api/v1/jobs/my THEN 200 + list of own jobs
```

**APIs Involved:** `POST /api/v1/jobs`, `PUT /api/v1/jobs/{id}`, `GET /api/v1/jobs/my`

**DB Changes:** Inserts/updates `jobs`

**Complexity:** M

---

### Task 4.3 — Job Feed with Filters & Keyset Pagination

**Description**
`GET /api/v1/jobs` is the core candidate experience. Returns a paginated, filterable feed of active jobs. Excludes jobs already swiped by the requesting candidate. Uses keyset pagination for performance.

**Query parameters:** `category`, `location`, `remote` (boolean), `salaryMin`, `salaryMax`, `keyword` (full-text), `cursor` (opaque base64-encoded keyset)

**Feed ordering:** `boost_score DESC, created_at DESC, id DESC`

**Files to create/update:**
- `job/dto/JobFeedRequest.java` — query param DTO with filters
- `job/dto/JobFeedResponse.java` — `items: List<JobResponse>`, `nextCursor: String`, `hasMore: boolean`
- `job/repository/JobFeedRepository.java` — custom `@Query` with dynamic WHERE clauses and keyset pagination using `Specification` or native query
- `JobService.getJobFeed(UUID candidateId, JobFeedRequest): JobFeedResponse`
- `JobController.getJobFeed()` — `GET /api/v1/jobs`

**Acceptance Criteria**
- Returns only `status=ACTIVE` and non-deleted jobs
- Excludes jobs the authenticated candidate already swiped (LIKE or PASS)
- `keyword` filter uses PostgreSQL `search_vector @@ to_tsquery()`
- `nextCursor` in response enables fetching next page
- Passing `cursor` from previous response returns the next page with no duplicates
- Premium employer jobs appear before non-premium jobs

**Test Cases (TDD)**
```
JobServiceTest:
  - GIVEN 10 active jobs WHEN getJobFeed(page 1, size 5) THEN returns 5 jobs + hasMore=true
  - GIVEN candidate swiped job A WHEN getJobFeed() THEN job A not in results
  - GIVEN keyword="engineer" WHEN getJobFeed() THEN only jobs matching "engineer" returned
  - GIVEN salaryMin=80000 filter WHEN getJobFeed() THEN only jobs with salary_max >= 80000
  - GIVEN premium employer job + regular job WHEN getJobFeed() THEN premium job first

JobControllerTest:
  - GIVEN USER token WHEN GET /api/v1/jobs THEN 200 + items list
  - GIVEN valid cursor WHEN GET /api/v1/jobs?cursor={cursor} THEN 200 + next page
  - GIVEN no auth WHEN GET /api/v1/jobs THEN 401 (public browse not allowed — must be registered)
```

**APIs Involved:** `GET /api/v1/jobs`

**DB Changes:** Read only (uses indexes from V3 migration)

**Complexity:** L

---

### Task 4.4 — Get Job Detail & Close/Delete Job

**Description**
`GET /api/v1/jobs/{id}` returns full job detail for any authenticated user.
`DELETE /api/v1/jobs/{id}` soft-closes the job (employer owner only).

**Files to create/update:**
- `JobService.getJobById(UUID jobId): JobResponse` — throws `ResourceNotFoundException` if not found or deleted
- `JobService.closeJob(UUID employerId, UUID jobId)` — verify ownership; set `status=CLOSED`, `deletedAt=NOW()`
- `JobController` — `GET /api/v1/jobs/{id}`, `DELETE /api/v1/jobs/{id}`

**Acceptance Criteria**
- Any authenticated user can view a job's detail
- Soft-deleted job returns 404
- Only owning employer can close a job
- Closing a job does not delete applications already submitted

**Test Cases (TDD)**
```
JobServiceTest:
  - GIVEN existing job WHEN getJobById() THEN returns JobResponse
  - GIVEN non-existent id WHEN getJobById() THEN throws ResourceNotFoundException
  - GIVEN job owned by employer A WHEN employer B calls closeJob() THEN throws AccessDeniedException
  - GIVEN open job WHEN closeJob() THEN status=CLOSED, deletedAt set

JobControllerTest:
  - GIVEN valid jobId WHEN GET /api/v1/jobs/{id} THEN 200 + job details
  - GIVEN non-existent id WHEN GET /api/v1/jobs/{id} THEN 404
  - GIVEN owner employer WHEN DELETE /api/v1/jobs/{id} THEN 204
  - GIVEN non-owner employer WHEN DELETE /api/v1/jobs/{id} THEN 403
```

**APIs Involved:** `GET /api/v1/jobs/{id}`, `DELETE /api/v1/jobs/{id}`

**DB Changes:** Updates `jobs.status`, `jobs.deleted_at`

**Complexity:** S

---

## Epic 5 — Swipe System

---

### Task 5.1 — CandidateSwipe Entity & Repository

**Description**
Create `CandidateSwipe` entity and repository. This tracks every candidate swipe on a job.

**Files to create:**
- `swipe/domain/CandidateSwipe.java` — fields: `id`, `candidateId`, `jobId`, `action` (SwipeAction enum), `swipedAt`
- `swipe/domain/SwipeAction.java` — enum: `LIKE`, `PASS`
- `swipe/domain/EmployerSwipeAction.java` — enum: `LIKE`, `REJECT`
- `swipe/repository/CandidateSwipeRepository.java`
  - `findByCandidateIdAndJobId(UUID, UUID): Optional<CandidateSwipe>`
  - `existsByCandidateIdAndJobIdAndAction(UUID, UUID, SwipeAction): boolean`

**Acceptance Criteria**
- Unique constraint `(candidate_id, job_id)` prevents duplicate swipes at DB level
- `existsByCandidateIdAndJobIdAndAction(action=LIKE)` returns true only when candidate LIKEd

**Test Cases (TDD)**
```
CandidateSwipeRepositoryTest (@DataJpaTest):
  - GIVEN LIKE swipe saved WHEN existsByCandidateIdAndJobIdAndAction(LIKE) THEN true
  - GIVEN PASS swipe saved WHEN existsByCandidateIdAndJobIdAndAction(LIKE) THEN false
  - GIVEN swipe saved WHEN saving duplicate (same candidate+job) THEN throws DataIntegrityViolationException
```

**APIs Involved:** None (repository layer)

**DB Changes:** `candidate_swipes` table (created by Flyway V4)

**Complexity:** S

---

### Task 5.2 — Kafka SwipeEvent Producer

**Description**
Create the Kafka event publisher for swipe actions. All swipes (candidate and employer) go through this publisher.

**Files to create:**
- `swipe/event/SwipeEvent.java` — record: `eventId`, `actorId`, `actorRole`, `targetId`, `targetType`, `action`, `timestamp`
- `swipe/event/SwipeEventPublisher.java` — `publish(SwipeEvent)` wraps `KafkaTemplate.send("swipe.events", event)`
- `shared/config/KafkaConfig.java` — `ProducerFactory`, `KafkaTemplate`, topic configuration beans; enable idempotent producer

**Acceptance Criteria**
- Every swipe action publishes a `SwipeEvent` to `swipe.events` topic
- Kafka producer is idempotent (enable.idempotence=true)
- If Kafka is unreachable, swipe still saves to DB (async publish — log error, do not fail request)

**Test Cases (TDD)**
```
SwipeEventPublisherTest (Mockito + @EmbeddedKafka):
  - GIVEN a SwipeEvent WHEN publish() THEN message appears on swipe.events topic
  - GIVEN KafkaTemplate throws WHEN publish() THEN exception is caught and logged (not propagated)
```

**APIs Involved:** None (internal)

**DB Changes:** None

**Complexity:** S

---

### Task 5.3 — Candidate Swipe Endpoint + Redis Rate Limiting

**Description**
`POST /api/v1/swipes/jobs/{jobId}` — candidate swipes LIKE or PASS on a job. Free users are limited to 20 swipes/day enforced via Redis atomic counter.

**Files to create:**
- `swipe/dto/CandidateSwipeRequest.java` — `action: SwipeAction`
- `swipe/service/SwipeService.java`
  - `candidateSwipe(UUID candidateId, UUID jobId, SwipeAction action)`
  - Validate job exists and is ACTIVE
  - Check swipe limit via Redis: `INCR swipe:limit:{userId}:{date}` + `EXPIREAT {midnight}`; if > 20 and not premium → throw `SwipeLimitExceededException` (429)
  - Upsert `CandidateSwipe` record (ON CONFLICT — update action)
  - Publish `SwipeEvent`
- `swipe/controller/SwipeController.java` — `POST /api/v1/swipes/jobs/{jobId}`
- `shared/service/SwipeLimitService.java` — Redis counter logic isolated here

**Acceptance Criteria**
- Returns 200 on success with current swipe action
- 21st swipe for free user returns 429 with `upgradeUrl`
- Premium user can swipe beyond 20 with no 429
- Swiping same job twice updates the action (re-swipe allowed)
- Cannot swipe a closed/deleted job (410)

**Test Cases (TDD)**
```
SwipeLimitServiceTest (Mockito RedisTemplate):
  - GIVEN free user at 19 swipes WHEN incrementAndCheck() THEN not rate-limited
  - GIVEN free user at 20 swipes WHEN incrementAndCheck() THEN throws SwipeLimitExceededException
  - GIVEN premium user at 100 swipes WHEN incrementAndCheck() THEN not rate-limited

SwipeServiceTest:
  - GIVEN LIKE swipe on active job WHEN candidateSwipe() THEN CandidateSwipe saved with LIKE, event published
  - GIVEN PASS swipe on same job after LIKE WHEN candidateSwipe() THEN action updated to PASS
  - GIVEN closed job WHEN candidateSwipe() THEN throws ResourceNotFoundException (410)

SwipeControllerTest:
  - GIVEN USER token + valid jobId WHEN POST /api/v1/swipes/jobs/{jobId} body={action:LIKE} THEN 200
  - GIVEN free user over limit WHEN POST swipe THEN 429 + upgradeUrl
  - GIVEN EMPLOYER token WHEN POST /api/v1/swipes/jobs/{jobId} THEN 403
```

**APIs Involved:** `POST /api/v1/swipes/jobs/{jobId}`

**DB Changes:** Upserts into `candidate_swipes`

**Complexity:** M

---

### Task 5.4 — Employer Swipe Endpoint

**Description**
`POST /api/v1/swipes/applicants/{applicationId}` — employer swipes LIKE or REJECT on a candidate who has applied to one of their jobs. Cannot swipe on non-applicants.

**Files to create/update:**
- `swipe/domain/EmployerSwipe.java` — fields: `id`, `employerId`, `applicationId`, `candidateId`, `jobId`, `action`, `swipedAt`
- `swipe/repository/EmployerSwipeRepository.java`
  - `findByEmployerIdAndApplicationId(UUID, UUID): Optional<EmployerSwipe>`
  - `existsByCandidateIdAndJobIdAndAction(UUID, UUID, EmployerSwipeAction): boolean`
- `swipe/dto/EmployerSwipeRequest.java` — `action: EmployerSwipeAction`
- `SwipeService.employerSwipe(UUID employerId, UUID applicationId, EmployerSwipeAction action)`
  - Verify `application.jobId` belongs to this employer (403 if not)
  - Upsert `EmployerSwipe`
  - Publish `SwipeEvent`
- `SwipeController` — `POST /api/v1/swipes/applicants/{applicationId}`

**Acceptance Criteria**
- Employer can only swipe on applications for their own jobs
- Swiping on another employer's job's applicant returns 403
- Swipe on non-existent application returns 404
- Re-swiping updates the action

**Test Cases (TDD)**
```
SwipeServiceTest:
  - GIVEN employer owns job WHEN employerSwipe(LIKE) on valid application THEN EmployerSwipe saved, event published
  - GIVEN employer does NOT own the job WHEN employerSwipe() THEN throws AccessDeniedException
  - GIVEN non-existent applicationId WHEN employerSwipe() THEN throws ResourceNotFoundException

SwipeControllerTest:
  - GIVEN EMPLOYER token + own applicant WHEN POST /api/v1/swipes/applicants/{id} THEN 200
  - GIVEN USER token WHEN POST /api/v1/swipes/applicants/{id} THEN 403
  - GIVEN EMPLOYER + other employer's applicant WHEN POST swipe THEN 403
```

**APIs Involved:** `POST /api/v1/swipes/applicants/{applicationId}`

**DB Changes:** Upserts into `employer_swipes`

**Complexity:** M

---

### Task 5.5 — Premium: See Who Liked Your Job

**Description**
`GET /api/v1/swipes/jobs/{jobId}/likes` returns the list of candidates who swiped LIKE on this employer's job. Premium employer feature only.

**Files to create/update:**
- `swipe/dto/JobLikersResponse.java` — list of candidate summaries
- `SwipeService.getJobLikers(UUID employerId, UUID jobId): List<CandidateSummary>`
  - Check `SubscriptionService.isActive(employerId)` → 403 if not premium
  - Verify job belongs to employer
  - Query `candidate_swipes` for LIKE on this job
- `SwipeController` — `GET /api/v1/swipes/jobs/{jobId}/likes`

**Acceptance Criteria**
- Premium employer sees list of candidates who LIKEd their job
- Non-premium employer gets 403 (with upgrade message)
- Non-employer role gets 403
- Empty list returned if nobody liked yet (not 404)

**Test Cases (TDD)**
```
SwipeServiceTest:
  - GIVEN premium employer WHEN getJobLikers() THEN returns candidates who swiped LIKE
  - GIVEN non-premium employer WHEN getJobLikers() THEN throws PremiumRequiredException (403)
  - GIVEN no likes WHEN getJobLikers() THEN returns empty list

SwipeControllerTest:
  - GIVEN premium EMPLOYER token WHEN GET /api/v1/swipes/jobs/{jobId}/likes THEN 200
  - GIVEN non-premium EMPLOYER WHEN GET .../likes THEN 403 + upgradeUrl
```

**APIs Involved:** `GET /api/v1/swipes/jobs/{jobId}/likes`

**DB Changes:** Read only

**Complexity:** S

---

## Epic 6 — Application System

---

### Task 6.1 — Application Entity & Repository

**Description**
Create `Application` entity and repository. This is the bridge table between candidates and jobs.

**Files to create:**
- `application/domain/Application.java` — fields: `id`, `candidateId`, `jobId`, `coverLetter`, `resumeUrl`, `status`, `appliedAt`, `reviewedAt`
- `application/domain/ApplicationStatus.java` — enum: `PENDING`, `REVIEWED`, `SHORTLISTED`, `ACCEPTED`, `REJECTED`, `WITHDRAWN`
- `application/repository/ApplicationRepository.java`
  - `findByCandidateIdAndJobId(UUID, UUID): Optional<Application>`
  - `existsByCandidateIdAndJobId(UUID, UUID): boolean`
  - `findByJobId(UUID jobId, Pageable): Page<Application>`
  - `findByCandidateId(UUID candidateId, Pageable): Page<Application>`

**Acceptance Criteria**
- Unique constraint `(candidate_id, job_id)` enforced at DB level
- `existsByCandidateIdAndJobId()` enables the swipe-gate validation

**Test Cases (TDD)**
```
ApplicationRepositoryTest (@DataJpaTest):
  - GIVEN saved application WHEN existsByCandidateIdAndJobId() THEN true
  - GIVEN no application WHEN existsByCandidateIdAndJobId() THEN false
  - GIVEN 3 applications for job WHEN findByJobId() THEN returns page of 3
  - GIVEN duplicate application WHEN save() THEN throws DataIntegrityViolationException
```

**APIs Involved:** None (repository layer)

**DB Changes:** `applications` table (created by Flyway V5)

**Complexity:** S

---

### Task 6.2 — Apply to a Job

**Description**
`POST /api/v1/applications/jobs/{jobId}` — candidate submits application. Validates that candidate previously swiped LIKE on this job. Publishes `ApplicationEvent`.

**Files to create:**
- `application/dto/ApplyRequest.java` — `coverLetter` (optional), `resumeUrl` (optional — overrides profile resume)
- `application/dto/ApplicationResponse.java`
- `application/event/ApplicationEvent.java`
- `application/event/ApplicationEventPublisher.java`
- `application/service/ApplicationService.java`
  - `apply(UUID candidateId, UUID jobId, ApplyRequest): ApplicationResponse`
  - Check `candidateSwipeRepository.existsByCandidateIdAndJobIdAndAction(LIKE)` → throw `BusinessRuleException` (422) if candidate didn't LIKE first
  - Check job is `ACTIVE` and not deleted → 410 if closed
  - Check no existing application → 409 if duplicate
  - Save application, increment `jobs.application_count` atomically, publish `ApplicationEvent`
- `application/controller/ApplicationController.java` — `POST /api/v1/applications/jobs/{jobId}`

**Acceptance Criteria**
- Returns 201 with `ApplicationResponse`
- 422 if candidate never swiped LIKE on this job
- 409 if candidate already applied
- 410 if job is closed
- `application_count` incremented on the job

**Test Cases (TDD)**
```
ApplicationServiceTest:
  - GIVEN candidate swiped LIKE WHEN apply() THEN application saved with PENDING status, event published
  - GIVEN candidate swiped PASS (not LIKE) WHEN apply() THEN throws BusinessRuleException
  - GIVEN candidate never swiped WHEN apply() THEN throws BusinessRuleException
  - GIVEN duplicate application WHEN apply() THEN throws DuplicateApplicationException
  - GIVEN closed job WHEN apply() THEN throws ResourceNotFoundException

ApplicationControllerTest:
  - GIVEN USER token + prior LIKE swipe WHEN POST /api/v1/applications/jobs/{jobId} THEN 201
  - GIVEN no prior LIKE WHEN POST apply THEN 422
  - GIVEN duplicate application WHEN POST apply THEN 409
  - GIVEN EMPLOYER token WHEN POST apply THEN 403
```

**APIs Involved:** `POST /api/v1/applications/jobs/{jobId}`

**DB Changes:** Inserts into `applications`; updates `jobs.application_count`

**Complexity:** M

---

### Task 6.3 — View & Update Applications

**Description**
Candidate views their own applications. Employer views applicants for their job. Employer can update application status (REVIEWED, SHORTLISTED, etc.).

**Files to create/update:**
- `ApplicationService.getCandidateApplications(UUID candidateId, Pageable): Page<ApplicationResponse>`
- `ApplicationService.getJobApplications(UUID employerId, UUID jobId, Pageable): Page<ApplicationResponse>` — verifies job ownership
- `ApplicationService.updateStatus(UUID employerId, UUID applicationId, ApplicationStatus newStatus)` — verifies job ownership; cannot transition to invalid states
- `ApplicationController` — `GET /api/v1/applications/my`, `GET /api/v1/applications/jobs/{jobId}`, `PUT /api/v1/applications/{id}/status`

**Acceptance Criteria**
- Candidate sees only their own applications
- Employer sees only applications for jobs they own
- Employer cannot set status to `WITHDRAWN` (candidate-only transition)
- Status update publishes `ApplicationEvent` with new status

**Test Cases (TDD)**
```
ApplicationServiceTest:
  - GIVEN candidate with 3 applications WHEN getCandidateApplications() THEN returns page of 3
  - GIVEN employer views own job's applicants WHEN getJobApplications() THEN returns applicants
  - GIVEN employer tries to view another employer's job applicants WHEN getJobApplications() THEN throws AccessDeniedException
  - GIVEN employer updates status to REVIEWED WHEN updateStatus() THEN status changed, event published
  - GIVEN employer tries to set WITHDRAWN WHEN updateStatus() THEN throws BusinessRuleException

ApplicationControllerTest:
  - GIVEN USER token WHEN GET /api/v1/applications/my THEN 200 + own applications
  - GIVEN EMPLOYER token WHEN GET /api/v1/applications/jobs/{jobId} THEN 200 + applicants
  - GIVEN non-owner EMPLOYER WHEN GET /api/v1/applications/jobs/{jobId} THEN 403
```

**APIs Involved:** `GET /api/v1/applications/my`, `GET /api/v1/applications/jobs/{jobId}`, `PUT /api/v1/applications/{id}/status`

**DB Changes:** Updates `applications.status`, `applications.reviewed_at`

**Complexity:** M

---

## Epic 7 — Match Engine

---

### Task 7.1 — Match Entity & Repository

**Description**
Create `Match` entity and repository. Matches are immutable once created.

**Files to create:**
- `match/domain/Match.java` — fields: `id`, `candidateId`, `employerId`, `jobId`, `applicationId`, `status`, `matchedAt`
- `match/domain/MatchStatus.java` — enum: `ACTIVE`, `ARCHIVED`, `EXPIRED`
- `match/repository/MatchRepository.java`
  - `findByCandidateId(UUID, Pageable): Page<Match>`
  - `findByEmployerId(UUID, Pageable): Page<Match>`
  - `existsByCandidateIdAndJobId(UUID, UUID): boolean`

**Acceptance Criteria**
- Unique constraint `(candidate_id, job_id)` enforced at DB and application level
- `existsByCandidateIdAndJobId()` enables idempotency check before insert

**Test Cases (TDD)**
```
MatchRepositoryTest (@DataJpaTest):
  - GIVEN saved match WHEN findByCandidateId() THEN returns match
  - GIVEN duplicate match attempt WHEN save() THEN throws DataIntegrityViolationException
  - GIVEN existing match WHEN existsByCandidateIdAndJobId() THEN true
```

**APIs Involved:** None (repository layer)

**DB Changes:** `matches` table (created by Flyway V6)

**Complexity:** S

---

### Task 7.2 — Match Gate: Kafka Consumer & Match Logic

**Description**
The heart of the platform. Two Kafka consumers evaluate the three-condition match gate on every swipe and application event. If all three conditions are met, a match is created.

**Three conditions:**
1. `candidate_swipes` has `action=LIKE` for `(candidateId, jobId)`
2. `applications` has a record for `(candidateId, jobId)`
3. `employer_swipes` has `action=LIKE` for `(candidateId, jobId)`

**Files to create:**
- `match/service/MatchService.java`
  - `evaluateMatchGate(UUID candidateId, UUID jobId): Optional<Match>`
  - Queries all three conditions
  - If all true AND `matchRepository.existsByCandidateIdAndJobId()` is false → create match
  - `INSERT ... ON CONFLICT DO NOTHING` semantics via `save()` with try-catch `DataIntegrityViolationException`
  - If match created → publish `MatchEvent`
- `match/consumer/SwipeEventConsumer.java` — `@KafkaListener(topics="swipe.events", groupId="match-service")`
  - On `EMPLOYER LIKE` swipe event → call `evaluateMatchGate()`
- `match/consumer/ApplicationEventConsumer.java` — `@KafkaListener(topics="application.events", groupId="match-service")`
  - On `SUBMITTED` application event → call `evaluateMatchGate()`
- `match/event/MatchEvent.java`
- `match/event/MatchEventPublisher.java`

**Acceptance Criteria**
- Match created ONLY when all three conditions satisfied simultaneously
- If employer swipes LIKE before application exists → no match yet; match created when application arrives
- If application submitted after employer already liked → match created immediately
- Duplicate match creation attempt is a silent no-op (no exception, no duplicate row)
- Consumer commits offset only after successful processing (manual ack or default at-least-once)

**Test Cases (TDD)**
```
MatchServiceTest (Mockito):
  - GIVEN all 3 conditions met WHEN evaluateMatchGate() THEN match created, event published
  - GIVEN only LIKE swipe + application, no employer LIKE WHEN evaluateMatchGate() THEN no match
  - GIVEN only employer LIKE, no application WHEN evaluateMatchGate() THEN no match
  - GIVEN all conditions met but match already exists WHEN evaluateMatchGate() THEN no duplicate, no exception
  - GIVEN all conditions met WHEN evaluateMatchGate() called twice concurrently THEN exactly 1 match row

SwipeEventConsumerTest (@EmbeddedKafka):
  - GIVEN EMPLOYER LIKE event on swipe.events WHEN consumed THEN evaluateMatchGate() called

ApplicationEventConsumerTest (@EmbeddedKafka):
  - GIVEN SUBMITTED event on application.events WHEN consumed THEN evaluateMatchGate() called

MatchIntegrationTest (@SpringBootTest, Testcontainers PG + Kafka):
  - GIVEN candidate LIKEs job + applies + employer LIKEs WHEN events processed THEN match row exists in DB
  - GIVEN candidate LIKEs + applies, employer has NOT liked WHEN events processed THEN no match row
```

**APIs Involved:** None (event-driven)

**DB Changes:** Inserts into `matches`

**Complexity:** L

---

### Task 7.3 — View Matches API

**Description**
Both candidates and employers can list their matches and view match details.

**Files to create/update:**
- `match/dto/MatchResponse.java` — includes job summary, candidate/employer summary, matched date
- `match/service/MatchService.getMatchesForUser(UUID userId, UserRole role, Pageable): Page<MatchResponse>`
- `match/controller/MatchController.java` — `GET /api/v1/matches`, `GET /api/v1/matches/{id}`

**Acceptance Criteria**
- Candidate sees matches where they are the candidate
- Employer sees matches where they are the employer
- `GET /api/v1/matches/{id}` returns 404 if the requesting user is not a participant in the match
- Results sorted by `matched_at DESC`

**Test Cases (TDD)**
```
MatchServiceTest:
  - GIVEN candidate with 2 matches WHEN getMatchesForUser(USER role) THEN returns 2 matches
  - GIVEN employer with 3 matches WHEN getMatchesForUser(EMPLOYER role) THEN returns 3 matches

MatchControllerTest:
  - GIVEN USER token with matches WHEN GET /api/v1/matches THEN 200 + match list
  - GIVEN USER token requesting match they're not part of WHEN GET /api/v1/matches/{id} THEN 404
```

**APIs Involved:** `GET /api/v1/matches`, `GET /api/v1/matches/{id}`

**DB Changes:** Read only

**Complexity:** S

---

## Epic 8 — Notification System

---

### Task 8.1 — Notification Entity & Repository

**Description**
Create `Notification` entity and repository for in-app notifications.

**Files to create:**
- `notification/domain/Notification.java` — fields: `id`, `userId`, `type`, `title`, `body`, `referenceId`, `referenceType`, `isRead`, `readAt`, `createdAt`
- `notification/domain/NotificationType.java` — enum: `MATCH_CREATED`, `APPLICATION_RECEIVED`, `APPLICATION_STATUS_CHANGED`, `SWIPE_RECEIVED`, `SUBSCRIPTION_RENEWED`, `SUBSCRIPTION_EXPIRING`
- `notification/repository/NotificationRepository.java`
  - `findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID, Pageable)`
  - `findByUserIdOrderByCreatedAtDesc(UUID, Pageable)`
  - `countByUserIdAndIsReadFalse(UUID): long`

**Acceptance Criteria**
- Unread count query uses the partial index `idx_notif_user_unread`
- Notifications are ordered newest first

**Test Cases (TDD)**
```
NotificationRepositoryTest (@DataJpaTest):
  - GIVEN 5 notifications (3 unread) WHEN countByUserIdAndIsReadFalse() THEN 3
  - GIVEN mixed read/unread WHEN findByUserIdAndIsReadFalseOrderByCreatedAtDesc() THEN only unread returned
```

**APIs Involved:** None (repository layer)

**DB Changes:** `notifications` table (created by Flyway V8)

**Complexity:** S

---

### Task 8.2 — Notification Kafka Consumers

**Description**
Two Kafka consumers create in-app notifications in response to match and application events.

**Files to create:**
- `notification/consumer/MatchEventConsumer.java` — `@KafkaListener(topics="match.events", groupId="notification-service")`
  - On match event → create two notifications: one for candidate ("You matched with {company}!"), one for employer ("New match for {job title}!")
- `notification/consumer/ApplicationEventConsumer.java` — `@KafkaListener(topics="application.events", groupId="notification-service")`
  - On `SUBMITTED` → notify employer ("New application received for {job}")
  - On `ACCEPTED` → notify candidate ("Your application to {company} was accepted!")
  - On `REJECTED` → notify candidate ("Your application to {company} was not selected")
- `notification/service/NotificationService.java` — `createNotification(UUID userId, NotificationType, String title, String body, UUID referenceId, String referenceType)`

**Acceptance Criteria**
- Match event creates exactly 2 notifications (one per party)
- Application status change creates 1 notification (to the affected candidate)
- Consumer failure does not crash the application — logged and sent to DLT
- Notifications are created even if the consumer processes the event after a delay

**Test Cases (TDD)**
```
NotificationServiceTest (Mockito):
  - GIVEN valid params WHEN createNotification() THEN notification saved with isRead=false

MatchEventConsumerTest (@EmbeddedKafka):
  - GIVEN match.events receives MatchEvent WHEN consumed THEN 2 notifications created in DB

ApplicationEventConsumerTest (@EmbeddedKafka):
  - GIVEN ACCEPTED status event WHEN consumed THEN candidate notification created
  - GIVEN consumer throws exception WHEN event processed THEN message routed to DLT
```

**APIs Involved:** None (event-driven)

**DB Changes:** Inserts into `notifications`

**Complexity:** M

---

### Task 8.3 — Notification REST Endpoints

**Description**
Endpoints for users to view and manage their in-app notifications.

**Files to create/update:**
- `notification/dto/NotificationResponse.java`
- `notification/dto/NotificationPageResponse.java` — includes `unreadCount`
- `notification/controller/NotificationController.java`
  - `GET /api/v1/notifications` — paginated, newest first; includes unreadCount in response
  - `PUT /api/v1/notifications/{id}/read` — mark single notification as read
  - `PUT /api/v1/notifications/read-all` — mark all as read

**Acceptance Criteria**
- Users only see their own notifications
- `unreadCount` in response is accurate
- Marking already-read notification as read is idempotent (no error)
- Marking non-existent notification returns 404

**Test Cases (TDD)**
```
NotificationControllerTest (@WebMvcTest):
  - GIVEN user with 5 notifications WHEN GET /api/v1/notifications THEN 200 + 5 items + unreadCount
  - GIVEN unread notification WHEN PUT /api/v1/notifications/{id}/read THEN 200 + isRead=true
  - GIVEN already-read notification WHEN PUT .../read THEN 200 (idempotent)
  - GIVEN non-existent notif id WHEN PUT .../read THEN 404
  - GIVEN user with 3 unread WHEN PUT /api/v1/notifications/read-all THEN 200 + all marked read
```

**APIs Involved:** `GET /api/v1/notifications`, `PUT /api/v1/notifications/{id}/read`, `PUT /api/v1/notifications/read-all`

**DB Changes:** Updates `notifications.is_read`, `notifications.read_at`

**Complexity:** S

---

## Epic 9 — Premium / Subscription

---

### Task 9.1 — Subscription Entity & SubscriptionService

**Description**
Create `Subscription` entity, repository, and the central `SubscriptionService.isActive()` method that all premium feature checks must use.

**Files to create:**
- `subscription/domain/Subscription.java` — fields: `id`, `userId`, `plan`, `status`, `stripeCustomerId`, `stripeSubscriptionId`, `currentPeriodStart`, `currentPeriodEnd`, `cancelledAt`
- `subscription/domain/SubscriptionPlan.java` — enum: `PREMIUM_MONTHLY`, `PREMIUM_ANNUAL`
- `subscription/domain/SubscriptionStatus.java` — enum: `ACTIVE`, `CANCELLED`, `EXPIRED`, `PAST_DUE`
- `subscription/repository/SubscriptionRepository.java`
  - `findTopByUserIdAndStatusOrderByCurrentPeriodEndDesc(UUID, SubscriptionStatus): Optional<Subscription>`
  - `findByStripeSubscriptionId(String): Optional<Subscription>`
- `subscription/service/SubscriptionService.java`
  - `isActive(UUID userId): boolean` — checks Redis cache first (`sub:active:{userId}` TTL 5 min), then DB
  - `syncPremiumFlag(UUID userId, boolean isPremium)` — updates `users.is_premium` + invalidates cache

**Acceptance Criteria**
- `isActive()` returns `true` only if an ACTIVE subscription exists with `currentPeriodEnd` in the future
- `isActive()` reads from Redis cache to avoid DB hit on every request
- Premium flag on `users` table is always in sync with subscription status (reconciled on webhook and by daily job)

**Test Cases (TDD)**
```
SubscriptionServiceTest (Mockito):
  - GIVEN active subscription in DB WHEN isActive() THEN true
  - GIVEN subscription expired WHEN isActive() THEN false
  - GIVEN no subscription WHEN isActive() THEN false
  - GIVEN cached result WHEN isActive() called twice THEN repository called only once
  - GIVEN subscription activated WHEN syncPremiumFlag() THEN users.is_premium=true, cache invalidated
```

**APIs Involved:** None (internal service)

**DB Changes:** `subscriptions` table (created by Flyway V7)

**Complexity:** M

---

### Task 9.2 — Stripe Integration: Checkout & Webhook

**Description**
Create Stripe checkout session for premium plan purchase. Handle Stripe webhook events to activate/deactivate subscriptions.

**Files to create:**
- `subscription/dto/CheckoutRequest.java` — `plan: SubscriptionPlan`
- `subscription/dto/CheckoutResponse.java` — `checkoutUrl: String`
- `subscription/service/StripeWebhookService.java`
  - `handleEvent(String payload, String signature)` — verify Stripe signature
  - Handle `checkout.session.completed` → create `Subscription` record, call `syncPremiumFlag(true)`
  - Handle `customer.subscription.deleted` / `invoice.payment_failed` → update status, call `syncPremiumFlag(false)`
- `subscription/controller/SubscriptionController.java`
  - `POST /api/v1/subscriptions/checkout` — create Stripe session, return `checkoutUrl`
  - `POST /api/v1/subscriptions/webhook` — receive Stripe event (no JWT required, Stripe-Signature header instead)
  - `GET /api/v1/subscriptions/me` — current subscription status
  - `GET /api/v1/subscriptions/plans` — public plan listing

**Acceptance Criteria**
- Webhook signature verified before processing (reject tampered payloads with 400)
- Idempotent: processing same Stripe event twice does not create duplicate subscriptions
- After `checkout.session.completed`, user's `is_premium` is true within the same request cycle
- Webhook endpoint returns 200 quickly (heavy processing is synchronous but minimal — no Kafka needed here)

**Test Cases (TDD)**
```
StripeWebhookServiceTest (Mockito):
  - GIVEN valid checkout.session.completed event WHEN handleEvent() THEN subscription created, isPremium=true
  - GIVEN subscription.deleted event WHEN handleEvent() THEN subscription status=CANCELLED, isPremium=false
  - GIVEN invalid signature WHEN handleEvent() THEN throws SignatureVerificationException

SubscriptionControllerTest:
  - GIVEN authenticated user WHEN POST /api/v1/subscriptions/checkout THEN 200 + checkoutUrl
  - GIVEN tampered webhook payload WHEN POST /api/v1/subscriptions/webhook THEN 400
  - GIVEN valid webhook WHEN POST /api/v1/subscriptions/webhook THEN 200
  - GIVEN authenticated user WHEN GET /api/v1/subscriptions/me THEN 200 + subscription status
```

**APIs Involved:** `POST /api/v1/subscriptions/checkout`, `POST /api/v1/subscriptions/webhook`, `GET /api/v1/subscriptions/me`, `GET /api/v1/subscriptions/plans`

**DB Changes:** Inserts/updates `subscriptions`; updates `users.is_premium`

**Complexity:** L

---

### Task 9.3 — Scheduled Jobs: Expiry Sync & Boost Reset

**Description**
Two `@Scheduled` jobs run nightly: one reconciles expired subscriptions, one resets boost scores for jobs whose employer's premium has lapsed.

**Files to create:**
- `subscription/scheduler/SubscriptionExpiryScheduler.java`
  - `@Scheduled(cron="0 0 1 * * *")` — find all ACTIVE subscriptions where `currentPeriodEnd < NOW()`; set status=EXPIRED; call `syncPremiumFlag(userId, false)` for each
- `job/scheduler/BoostScoreScheduler.java`
  - `@Scheduled(cron="0 5 1 * * *")` — reset `boost_score=0` for jobs owned by non-premium employers; set `boost_score=100` for premium employers

**Acceptance Criteria**
- Expired subscriptions are marked EXPIRED within 1 hour of actual expiry
- Boost scores reflect current premium status within 1 day
- Scheduler logs every affected record

**Test Cases (TDD)**
```
SubscriptionExpirySchedulerTest (Mockito):
  - GIVEN 3 expired subscriptions WHEN runExpirySync() THEN all 3 set to EXPIRED, syncPremiumFlag called 3 times
  - GIVEN no expired subscriptions WHEN runExpirySync() THEN no changes made

BoostScoreSchedulerTest:
  - GIVEN non-premium employer's jobs with boostScore=100 WHEN resetBoostScores() THEN set to 0
  - GIVEN premium employer's jobs with boostScore=0 WHEN resetBoostScores() THEN set to 100
```

**APIs Involved:** None (scheduled)

**DB Changes:** Updates `subscriptions.status`; updates `jobs.boost_score`

**Complexity:** S

---

## Epic 10 — Admin Module

---

### Task 10.1 — Admin User Management

**Description**
Admin endpoints for listing users, banning accounts, and changing user roles.

**Files to create:**
- `admin/dto/AdminUserResponse.java` — includes all user fields including banned status
- `admin/dto/BanRequest.java` — `reason: String`
- `admin/dto/ChangeRoleRequest.java` — `role: UserRole`
- `admin/service/AdminService.java`
  - `listUsers(Pageable, String searchQuery): Page<AdminUserResponse>`
  - `banUser(UUID userId, String reason)` — set `isBanned=true`; blacklist all active tokens for user (requires iterating Redis)
  - `changeRole(UUID userId, UserRole newRole)` — cannot demote self; cannot promote to ADMIN via API (ADMIN must be set in DB)
- `admin/controller/AdminController.java`
  - `GET /api/v1/admin/users` — requires ADMIN role
  - `PUT /api/v1/admin/users/{id}/ban`
  - `PUT /api/v1/admin/users/{id}/role`

**Acceptance Criteria**
- All endpoints require ADMIN role (403 for any other role)
- Banning a user immediately invalidates their sessions (add userId-level deny-list key in Redis)
- Admin cannot ban themselves
- Cannot promote to ADMIN via the API (only ADMIN, EMPLOYER, USER, PREMIUM allowed in endpoint)

**Test Cases (TDD)**
```
AdminServiceTest:
  - GIVEN 10 users WHEN listUsers() THEN returns paginated list
  - GIVEN search query "john" WHEN listUsers() THEN returns matching users
  - GIVEN active user WHEN banUser() THEN isBanned=true in DB
  - GIVEN admin tries to ban self WHEN banUser() THEN throws BusinessRuleException
  - GIVEN USER WHEN changeRole(EMPLOYER) THEN role updated

AdminControllerTest:
  - GIVEN USER role token WHEN GET /api/v1/admin/users THEN 403
  - GIVEN ADMIN token WHEN GET /api/v1/admin/users THEN 200
  - GIVEN ADMIN token WHEN PUT /api/v1/admin/users/{id}/ban THEN 200
  - GIVEN ADMIN tries to set role=ADMIN via API WHEN PUT .../role THEN 400
```

**APIs Involved:** `GET /api/v1/admin/users`, `PUT /api/v1/admin/users/{id}/ban`, `PUT /api/v1/admin/users/{id}/role`

**DB Changes:** Updates `users.is_banned`, `users.role`

**Complexity:** M

---

### Task 10.2 — Admin Job Moderation

**Description**
Admin can force-remove any job posting regardless of ownership.

**Files to create/update:**
- `AdminService.forceCloseJob(UUID jobId)` — set `status=ARCHIVED`, `deletedAt=NOW()`
- `AdminController` — `DELETE /api/v1/admin/jobs/{id}`

**Acceptance Criteria**
- Job is soft-deleted (not hard deleted)
- Affected candidates who applied receive a notification that the job was removed
- Action is logged at INFO level with adminId + jobId

**Test Cases (TDD)**
```
AdminServiceTest:
  - GIVEN active job WHEN forceCloseJob() THEN status=ARCHIVED, deletedAt set
  - GIVEN non-existent job WHEN forceCloseJob() THEN throws ResourceNotFoundException

AdminControllerTest:
  - GIVEN ADMIN token WHEN DELETE /api/v1/admin/jobs/{id} THEN 204
  - GIVEN EMPLOYER token WHEN DELETE /api/v1/admin/jobs/{id} THEN 403
```

**APIs Involved:** `DELETE /api/v1/admin/jobs/{id}`

**DB Changes:** Updates `jobs.status`, `jobs.deleted_at`

**Complexity:** S

---

## Epic 11 — Infrastructure & Observability

---

### Task 11.1 — Structured Logging & Correlation IDs

**Description**
Replace default Spring Boot logging with structured JSON output. Inject a correlation ID (UUID) into MDC at the filter level. Propagate through Kafka message headers.

**Files to create:**
- `shared/filter/CorrelationIdFilter.java` — `OncePerRequestFilter`; reads `X-Correlation-ID` header or generates UUID; puts in MDC as `correlationId`; adds to response header
- `shared/kafka/KafkaCorrelationInterceptor.java` — `ProducerInterceptor` that adds `correlationId` from MDC into Kafka record headers
- `src/main/resources/logback-spring.xml` — JSON encoder via `logstash-logback-encoder`; include fields: `timestamp`, `level`, `correlationId`, `traceId`, `message`, `logger`

**Acceptance Criteria**
- Every log line is valid JSON
- `correlationId` present in every log line for a given request
- `correlationId` propagated into Kafka message headers
- Sensitive fields (password, token values) never appear in logs

**Test Cases (TDD)**
```
CorrelationIdFilterTest:
  - GIVEN request with X-Correlation-ID header WHEN filtered THEN MDC contains that value
  - GIVEN request without header WHEN filtered THEN MDC contains generated UUID
  - GIVEN filtered request WHEN response returned THEN X-Correlation-ID in response headers
```

**APIs Involved:** All endpoints

**DB Changes:** None

**Complexity:** S

---

### Task 11.2 — Actuator, Prometheus & Custom Metrics

**Description**
Enable Spring Actuator endpoints and add custom business metrics for monitoring.

**Files to create/update:**
- `shared/metrics/SwipeMetrics.java` — `@Component` using `MeterRegistry`
  - `Counter swipes_total` (tags: action=LIKE|PASS, role=CANDIDATE|EMPLOYER)
  - `Counter matches_created_total`
  - `Counter applications_submitted_total`
- `shared/metrics/PremiumMetrics.java`
  - `Gauge active_premium_users`
- `application.yml` — expose `health`, `info`, `prometheus` actuator endpoints; restrict others

**Acceptance Criteria**
- `GET /actuator/health` returns `{"status":"UP"}` with DB and Redis sub-indicators
- `GET /actuator/prometheus` returns Prometheus-format metrics including custom metrics
- Custom counters increment on every swipe, match, and application

**Test Cases (TDD)**
```
SwipeMetricsTest (Mockito MeterRegistry):
  - GIVEN LIKE swipe event WHEN recordSwipe(LIKE) THEN swipes_total{action=LIKE} incremented

ActuatorTest (@SpringBootTest):
  - GIVEN app running WHEN GET /actuator/health THEN 200 + status=UP
  - GIVEN app running WHEN GET /actuator/prometheus THEN 200 + contains "swipes_total"
```

**APIs Involved:** `GET /actuator/health`, `GET /actuator/prometheus`

**DB Changes:** None

**Complexity:** S

---

### Task 11.3 — Dockerfile (Multi-Stage Build)

**Description**
Create a production-ready multi-stage Dockerfile. Stage 1 builds the JAR. Stage 2 runs it on a minimal JRE image.

**File to create:** `Dockerfile` at project root

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=builder /app/target/*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "app.jar"]
```

**Acceptance Criteria**
- `docker build -t job-cupid:latest .` succeeds
- Container starts and `/actuator/health` returns UP (with injected env vars for DB/Redis/Kafka)
- Image runs as non-root user
- Final image is < 300MB

**Test Cases (TDD)**
```
(Manual verification: docker build + docker run + curl /actuator/health)
```

**APIs Involved:** None

**DB Changes:** None

**Complexity:** S

---

### Task 11.4 — Kubernetes Manifests

**Description**
Create K8s manifests for production deployment: Deployment, Service, HPA, ConfigMap, and Ingress.

**Files to create (in `k8s/` directory):**
- `deployment.yaml` — 3 replicas, resource requests/limits, liveness + readiness probes, env from Secret
- `service.yaml` — ClusterIP service on port 8080
- `hpa.yaml` — min 3, max 10 replicas; scale on CPU > 70%
- `configmap.yaml` — non-secret config (app name, Kafka topic names, log level)
- `secret.yaml` — template (values replaced by CI/CD from AWS Secrets Manager)
- `ingress.yaml` — TLS termination, routes `/api/**` and `/auth/**` to service
- `pdb.yaml` — `PodDisruptionBudget` with `minAvailable: 2`

**Acceptance Criteria**
- `kubectl apply -f k8s/` applies all manifests without errors on a test cluster
- Liveness probe on `/actuator/health/liveness` (failureThreshold: 3, periodSeconds: 10)
- Readiness probe on `/actuator/health/readiness` (failureThreshold: 3, periodSeconds: 5)
- HPA scales up when CPU > 70% on a load test

**Test Cases (TDD)**
```
(Verified via: kubectl apply --dry-run=client -f k8s/)
```

**APIs Involved:** None

**DB Changes:** None

**Complexity:** M

---

## Summary

| Epic | Tasks | Total Complexity |
|------|-------|-----------------|
| 1. Foundation | 6 tasks | S S S M S S |
| 2. Authentication | 6 tasks | M M M S S S |
| 3. User Profiles | 5 tasks | S S M S M |
| 4. Job Management | 4 tasks | S M L S |
| 5. Swipe System | 5 tasks | S S M M S |
| 6. Application System | 3 tasks | S M M |
| 7. Match Engine | 3 tasks | S L S |
| 8. Notification System | 3 tasks | S M S |
| 9. Premium / Subscription | 3 tasks | M L S |
| 10. Admin Module | 2 tasks | M S |
| 11. Infrastructure | 4 tasks | S S S M |

**Total tasks: 44**
**L tasks (most complex, tackle carefully): 4.3 (Job Feed), 7.2 (Match Gate), 9.2 (Stripe)**

---

## Implementation Rules

1. **Write the test first, always.** If you cannot write the test first, the acceptance criteria are not clear enough — clarify before coding.
2. **One PR per task.** No bundling unrelated work.
3. **Never bypass the subscription check.** All premium gates must call `SubscriptionService.isActive()` — never read `user.isPremium` directly in business logic.
4. **Never return `passwordHash` in any DTO.** Ever.
5. **Kafka publish is fire-and-forget.** A Kafka failure must never cause the main HTTP response to fail. Log and alert; don't propagate.
6. **Migrations are append-only.** Never modify an existing Flyway migration file that has already been applied in any environment.
7. **All DB queries use indexes.** Before writing a new query, verify the WHERE clause is covered by an existing index.
