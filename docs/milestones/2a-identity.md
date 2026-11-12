# Milestone 2A — Identity implementation report

Follow-up: [Java quality-gate report](2a-quality-gate.md) records the subsequent style/coverage review. The verification counts below are the original implementation run.

Date: 2026-11-12. Assessment: **READY FOR REVIEW**. Public launch remains blocked as described below.

## A. Delivered scope

Identity now supports persistent account registration, Argon2id credentials, server-side JDBC sessions, explicit CSRF initialization, login, current-account lookup and logout. Disabled accounts cannot log in or continue protected requests. Foundation status/health, the typed frontend adapter and the existing toolchain remain intact.

Only identity was introduced. AGENTS.md now explicitly authorizes 2A and stops before 2B/2C. No gameplay, character tables, currencies, login UI, commit, push or deployment was added.

## B. Added packages and responsibilities

All paths below are relative to `backend/src/main/java/com/steelstory/`; each named public type has its own Java file.

```text
identity/
  package-info.java                 closed Modulith module; no allowed dependencies
  api/
    package-info.java               named interface "api"
    AccountId.java                  non-null stable UUID value
    AccountAccess.java              current account eligibility contract
  internal/
    domain/
      Account.java                  immutable account, required values/time ordering, eligibility
      AccountStatus.java            ACTIVE / DISABLED
      EmailAddress.java             validated, canonical ASCII email value
      PasswordHash.java             Argon2id hash value with redacted toString
      PasswordPolicy.java           bounded Unicode password validation
      InvalidIdentityInputException.java     safe input-validation failure
    application/
      IdentityService.java          registration transaction and authentication/read use cases
      AccountRepository.java        persistence port for the needed account operations
      PasswordHashing.java           cryptographic adapter boundary, including dummy verification
      EmailAlreadyRegisteredException.java   explicit registration conflict
      InvalidCredentialsException.java       uniform credential failure
    infrastructure/
      JdbcAccountRepository.java    bound SQL, unique insert, safe account reconstruction
      Argon2PasswordHashing.java     Spring Argon2id hashing with random salts
      AccountAuthenticationProvider.java
                                    Spring provider; UUID principal, null stored credentials
    web/
      AuthController.java           HTTP routing, DTO conversion, delegation
      SessionAuthentication.java    standard session/CSRF rotation, save and invalidation
      IdentityExceptionHandler.java safe 400/401/409 ProblemDetail mapping
      dto/
        CredentialsRequest.java     bounded documented JSON input; redacted toString
        AccountResponse.java        safe account fields only
        CsrfResponse.java           header/token response; redacted toString
        IdentityProblemResponse.java
                                    documentation of flattened RFC 9457 error JSON
bootstrap/security/
  SecurityConfiguration.java        SecurityFilterChain, Clock and framework wiring
  ActiveAccountFilter.java          fresh eligibility check through identity::api
  IdentityRequestLimitFilter.java   4096-byte bound, including chunked JSON bodies
  SecurityProblemWriter.java        sanitized filter-level RFC 9457 responses
```

The only nested implementation type is a private bounded-request wrapper. There are no nested public DTOs.

## C. Architectural decisions

1. **JDBC sessions:** use the existing PostgreSQL deployment for shared durable sessions; avoid Redis, JWT and sticky routing. Flyway owns the framework schema.
2. **Explicit module boundary:** bootstrap sees only identity::api and standard Spring interfaces. Identity imports no bootstrap or future gameplay modules. AccountAccess is a current read, not a mutation lock.
3. **Short registration transaction:** validation/Argon2 happen first; an application-owned transaction inserts through the module repository. Database uniqueness decides concurrent conflicts.
4. **Minimal principal:** persist UUID identity and framework security/CSRF state, never domain accounts, emails or credential hashes.
5. **Explicit CSRF endpoint:** default masked-token handling; header plus session cookie on mutations; refresh after login/logout.
6. **Fresh eligibility:** re-read account status for protected requests. This prevents a previously authenticated DISABLED account from continuing on its next request.
7. **Documented API evolution:** add five auth paths, their HTTP DTOs/errors/CSRF headers and cookie security scheme. The previous status path and StatusResponse were compared structurally and remain identical.

Rationale, alternatives, transaction limits and security implications are in [ADR 0003](../adr/0003-identity-sessions-and-security.md).

## D. OOP, SOLID, DRY, KISS and DDD

Account and its immutable values protect construction invariants; controllers cannot arbitrarily replace IDs, status or hashes. Domain code is independent of Spring/SQL/JSON. The application service coordinates operations, the repository owns SQL and the session adapter owns HTTP security lifecycle.

Interfaces are limited to a real module contract and two infrastructure boundaries. Services remain concrete. Composition connects these responsibilities; there is no inheritance hierarchy, generic CRUD/base layer or speculative module. Password/email policies have one domain implementation used by both registration and login. Shared credential and response DTOs serve the actual identical HTTP shapes.

## E. Flyway migrations

| Migration | Effect |
| --- | --- |
| V0001__foundation_create_schema.sql | Existing migration unchanged |
| V0002__identity_create_accounts.sql | UUID key; canonical email uniqueness; NOT NULL/length, hash/status/time constraints; timestamptz |
| V0003__identity_create_sessions.sql | Framework-compatible PostgreSQL session/attribute tables; session ID, expiry and principal indexes |

Tests apply the complete sequence to an empty database and upgrade a V0001 database containing a probe row. The row and V0001 checksum survive. No clean, repair or data reset was used.

## F. Endpoints and examples

Use the cookie-preserving [Postman walkthrough in README](../../README.md#identity-locally-and-in-postman).

| Method and path | Example input / result |
| --- | --- |
| GET /api/v1/auth/csrf | 200: headerName X-CSRF-TOKEN and token; retain SESSION cookie |
| POST /api/v1/auth/register | Synthetic email/password JSON plus CSRF; 201 AccountResponse, no auto-login |
| POST /api/v1/auth/login | Same JSON plus CSRF; 200 AccountResponse, rotated SESSION cookie |
| GET /api/v1/auth/me | Session cookie only; 200 own AccountResponse, or 401 |
| POST /api/v1/auth/logout | Fresh CSRF header and session cookie; 204, then ME returns 401 |

Synthetic local JSON:

```json
{"email":"player@example.test","password":"local-only example passphrase"}
```

Fetch a new CSRF token after login/logout. Registration conflicts return 409. Invalid input returns 400, bad credentials 401, missing/invalid CSRF 403 and identity POST bodies above 4096 bytes 413. Error bodies use application/problem+json. Public GET /api/v1/status and /actuator/health remain available.

Local startup remains:

```powershell
# Repository root
docker compose up -d --wait

# Backend terminal
cd backend
# JAVA_HOME selects your installed compatible JDK 25; no vendor-specific path is required.
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"

# Frontend terminal, pinned Node/npm
cd frontend
npm ci
npm run dev
```

The standard backend port is 8080, Vite 5173, and existing Postgres/port overrides and VS Code launch settings remain supported. SESSION_IDLE_TIMEOUT defaults to 12h. Secure cookies default to true outside local HTTP.

## G. Executed verification and resolved versions

Executed on Windows with Oracle JDK 25.0.2, Maven 3.9.16, Node 24.22.2, npm 10.8.2, Docker Engine 29.0.1 and Compose 2.40.3-desktop.1. This historical run selected Oracle 25.0.2 through JAVA_HOME; it is not a prerequisite. The later quality gate uses the developer's existing Temurin 25.0.5.

Backend commands, from backend/:

```powershell
# JAVA_HOME selects your installed compatible JDK 25; no vendor-specific path is required.
.\mvnw.cmd --version
.\mvnw.cmd --batch-mode --no-transfer-progress dependency:tree "-DoutputFile=target/identity-dependencies.txt"
.\mvnw.cmd --batch-mode --no-transfer-progress compile
.\mvnw.cmd --batch-mode --no-transfer-progress test
.\mvnw.cmd --batch-mode --no-transfer-progress verify
```

The final verify command redirected output to target/identity-verify-final.log and preserved Maven's exit status. Result: **BUILD SUCCESS**, **29 unit/architecture tests + 12 integration tests = 41**, **zero failures, errors or skips**.

| Test class | Executed cases |
| --- | ---: |
| ArchitectureTest | 2 |
| ApiWebTest | 3 |
| IdentityDomainTest | 23 |
| Argon2PasswordHashingTest | 1 |
| FoundationIT | 3 |
| IdentityIT | 8 |
| IdentityMigrationIT | 1 |

Real PostgreSQL tests verify registration, uniqueness/case normalization, concurrent duplicate requests, schema constraints, JDBC session contents/idle timeout, fixation protection, token rotation, successful/failed authentication, missing accounts, DISABLED login and session revocation, ME ownership, logout/replay, expired sessions, malformed/bounded input, missing/invalid CSRF, unknown protected routes, public status/health, Secure cookie defaults, and session reuse across independent application contexts and context restarts.

The first verify run failed only because the reviewed snapshot still contained Foundation. A subsequent targeted export used:

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress verify "-Dit.test=FoundationIT"
```

It also failed only on the intentionally outdated snapshot. Export review caught and fixed an unwanted framework CsrfToken query parameter and missing header schema; error documentation was aligned with flattened ProblemDetail JSON. A structural Node check confirmed the old status path/schema were unchanged before writing the new snapshot. Full verification then passed, including the contract comparison. Final review aligned account creation time with PostgreSQL microsecond precision and added register/login response equality to the existing HTTP test; verification was rerun afterward. No test was disabled and the contract assertion was retained.

Frontend commands, from frontend/, using pinned Node:

```powershell
npm exec --yes --package=npm@10.8.2 -- npm --version
npm exec --yes --package=npm@10.8.2 -- npm ci
npm exec --yes --package=npm@10.8.2 -- npm run typecheck
npm exec --yes --package=npm@10.8.2 -- npm run lint
npm exec --yes --package=npm@10.8.2 -- npm run build
```

All succeeded. npm ci reported zero vulnerabilities; that is an npm audit result, not a complete application security assessment. No frontend source or lockfile change was needed.

Repository/tool checks: git status --short, git diff and git diff --check; java -version, node --version, npm --version, docker info --format '{{.ServerVersion}}', docker compose version and docker compose ps. Inspected the resolved Spring Session 4.1.1 schema-postgresql.sql directly from its JAR. Final whitespace and changed-file review passed. The running local Compose database was inspected; new migration execution was verified in isolated Testcontainers databases.

| Resolved dependency | Version |
| --- | --- |
| Spring Boot / added starters | 4.1.2 |
| Spring Security | 7.1.2 |
| Spring Session JDBC/core | 4.1.1 |
| Bouncy Castle bcprov-jdk18on (new explicit pin) | 1.86 |
| Spring Modulith | 2.1.2 |
| Spring Framework / JDBC | 7.0.10 |
| Flyway | 12.4.0 |
| PostgreSQL server / JDBC | 18.6 / 42.7.14 |
| springdoc | 3.2.0 |
| Testcontainers | 2.0.5 |
| JUnit Jupiter / Mockito | 6.0.3 / 5.23.0 |
| React / TypeScript / Vite | 19.3.1 / 7.0.2 / 8.3.6 |
| Tailwind / Oxlint | 4.3.5 / 1.93.0 |

Official compatibility sources and historical versions are recorded in [ADR 0002](../adr/0002-tech-stack.md#milestone-2a-compatibility-amendment-2026-11-12).

## H. Risks and limitations

- **PUBLIC LAUNCH BLOCKER:** no effective cross-instance brute-force, credential-stuffing, registration or anonymous-session admission controls. Hashing/body bounds and generic login errors do not replace them.
- Duplicate-email 409 intentionally permits registration-status discovery. Email ownership is not yet verified.
- Status checks protect the next request; they do not cancel already admitted work. Future sensitive mutations need explicit transaction guards.
- JDBC session serialization assumes a trusted database and compatible application/library versions. Dependency upgrades require session compatibility review.
- Production HTTPS/ingress, operational limits, least-privilege migration/runtime roles and backup/restore processes are not delivered here.
- Hosted GitHub Actions/Linux execution, real multi-JVM/load-balancer/TLS deployment and manual Postman/browser authentication checks were not run. Tests exercised real HTTP/PostgreSQL and independent application contexts within the test JVM.
- Reset/recovery, email verification/change, account deletion, MFA, admin operations and OAuth remain future requirements. Authentication UI belongs to 2C.

## I. Changed files

Added Java classes and package-info files are enumerated in the tree in section B.

Additional new files:

- backend/src/main/resources/db/migration/V0002__identity_create_accounts.sql
- backend/src/main/resources/db/migration/V0003__identity_create_sessions.sql
- backend/src/test/java/com/steelstory/identity/IdentityIT.java
- backend/src/test/java/com/steelstory/identity/IdentityMigrationIT.java
- backend/src/test/java/com/steelstory/identity/internal/domain/IdentityDomainTest.java
- backend/src/test/java/com/steelstory/identity/internal/infrastructure/Argon2PasswordHashingTest.java
- docs/adr/0003-identity-sessions-and-security.md
- docs/milestones/2a-identity.md

Modified files:

- .github/workflows/ci.yaml — require Foundation and Identity integration reports
- AGENTS.md — explicit 2A authorization and stop
- README.md — current behavior, local/Postman workflow and blockers
- backend/pom.xml — security/session starters and Bouncy Castle
- backend/src/main/resources/application.yaml — durable sessions and secure cookie defaults
- backend/src/main/resources/application-local.yaml — local HTTP cookie setting
- backend/src/main/java/com/steelstory/bootstrap/web/ApiExceptionHandler.java — avoid sensitive exception-message logging
- backend/src/main/java/com/steelstory/bootstrap/web/OpenApiConfiguration.java — session cookie security scheme
- backend/src/test/java/com/steelstory/ArchitectureTest.java — one real module and stronger dependency checks
- backend/src/test/java/com/steelstory/FoundationIT.java — new migration baseline and Secure cookie check
- docs/architecture.md — implemented identity boundary/security
- docs/domain-model.md — initial Account policy
- docs/development-guidelines.md — security/logging conventions and next migration number
- docs/adr/0001-modular-monolith.md — separate 2A scope amendment
- docs/adr/0002-tech-stack.md — resolved 2A dependencies and verification
- docs/api/openapi.json — deliberate auth contract extension, formatted for review

V0001, StatusController, StatusResponse, Compose and frontend sources are unchanged.

## J. Assessment

**READY FOR REVIEW** for Milestone 2A. The implemented acceptance behavior and required local checks pass. This is not production/public-launch approval. Stop at 2A; no 2B/2C work was started.
