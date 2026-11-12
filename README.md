# Steelstory

Steelstory is an asynchronous multiplayer browser RPG. The architecture is **approved**. Milestone 1 provides the technical foundation; Milestone 2A adds accounts, session authentication and CSRF protection. Milestone 2B.2 adds character creation and own-character reads. Milestone 2B.3 provides a Polish, classic fantasy browser frontend with registration, login, three character slots, creation and character profiles. Further gameplay remains deferred.

## Prerequisites

Use a compatible **JDK 25** without preview features (including **Temurin 25.0.5**), Maven **3.9.16** via the included Wrapper **3.3.4** (no global Maven required), Node **24.22.2** (see `.nvmrc`), npm **10.8.2**, and a running Docker daemon with Compose. Oracle JDK 25.0.2 was the original local verification environment, not a required vendor, patch or installation path. CI retains its Linux/Temurin 25.0.2 pin for reproducibility; hosted CI has not yet run.

Set `JAVA_HOME` to your JDK 25 installation and put its `bin` directory on PATH. Switch to the pinned Node version using your usual version manager, then verify:

```text
java -version
node --version
npm --version
docker info
docker compose version
```

If necessary, select npm with `npm install --global npm@10.8.2`. The first Maven/npm execution requires registry access. On Windows use `.\mvnw.cmd` in place of `./mvnw`.

## Run locally

From the repository root:

```sh
docker compose up -d --wait
```

Compose runs PostgreSQL 18.6, pinned by digest, with a named volume mounted at PostgreSQL 18's `/var/lib/postgresql` data root. The database is `steelstory`, user `steelstory_local`, password `local-development-only`. These are disposable local credentials, never production credentials. The database port binds only to loopback.

In a second terminal:

```sh
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

PowerShell equivalent:

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

In a third terminal:

```sh
cd frontend
npm ci
npm run dev
```

Open [the frontend](http://127.0.0.1:5173). Register, then log in separately to create and select your characters. The frontend manages cookies and CSRF automatically; no manual tokens are needed. Vite proxies `/api` to `http://127.0.0.1:8080`, retaining the host-only SESSION cookie on the browser's origin. Always use `127.0.0.1:5173` consistently. Profiles use `#/characters/{id}` so refresh preserves navigation. The server connection check remains available in the shared frame. Production hosting must provide equivalent same-origin routing; production deployment is not implemented. Use `npm run dev` for the integrated local experience; `vite preview` is only a build preview, not the documented API environment.

See [the frontend delivery and test guide](docs/milestones/2b3-character-frontend.md) for manual acceptance steps, architecture, failure handling and browser tests.

If port 5432 is occupied, set `POSTGRES_PORT=55432` for Compose and `DB_URL=jdbc:postgresql://localhost:55432/steelstory` for the backend. This was the configuration used during local verification. In PowerShell:

```powershell
$env:POSTGRES_PORT = '55432'
docker compose up -d --wait
$env:DB_URL = 'jdbc:postgresql://localhost:55432/steelstory'
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Spring Boot does not automatically load Compose's `.env` file; export backend environment variables in its terminal. Other overrides include `DB_USERNAME`, `DB_PASSWORD` and `SERVER_PORT`. Changing the backend port also requires updating the Vite proxy target.

Stop application terminals with Ctrl+C. `docker compose stop` preserves database data. `docker compose down` removes containers but keeps the named volume; adding `--volumes` destroys local data. Stop the packaged backend before rebuilding its JAR on Windows to avoid a file-lock failure.

## API and database

REST request/response types use the `Dto` suffix and explicit web-layer mapping to application commands and safe response fields. Controllers return typed `ResponseEntity` values. Architecture tests enforce this transport boundary; reviewed JSON/OpenAPI and identity behavior are preserved. See [ADR 0004](docs/adr/0004-rest-dto-boundary.md) and the [refactoring verification report](docs/milestones/2a-dto-boundary.md).

- `GET http://localhost:8080/api/v1/status` returns `{"application":"steelstory","status":"UP"}`. This verifies API connectivity; it is not a database health report.
- `GET http://localhost:8080/actuator/health` checks application/database health without exposing details. Other Actuator endpoints are not exposed.
- `GET http://localhost:8080/v3/api-docs` exports OpenAPI 3.1 in the local profile. The reviewed contract is [docs/api/openapi.json](docs/api/openapi.json). No Swagger UI or generated client is installed.
- Unhandled failures return a sanitized RFC 9457 problem with `INTERNAL_ERROR`; framework errors preserve their HTTP status.

Flyway applies V0001 (the unchanged foundation namespace), V0002 (identity accounts), V0003 (JDBC sessions) and V0004 (email invariants). Migration history stays in `public`; identity tables live in `steelstory`. Applied migrations are immutable; startup validates checksums and fails on errors. V0004 validates existing email values and fails if invalid rows exist; it never rewrites or deletes account identities. SQL and Spring Session schema auto-initialization are disabled.

Without the local profile, `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` are required; docs are disabled and session cookies require HTTPS. Optional `FLYWAY_USERNAME`/`FLYWAY_PASSWORD` allow a separately provisioned migration role. Local development uses one role. Production role provisioning, migration release orchestration and TLS/hosting remain future work; this is not a production deployment configuration.

## Identity locally and in Postman

Milestone 2B.2 also provides authenticated POST/GET /api/v1/characters and GET /api/v1/characters/{id}.
Creation accepts name, characterClass and startingAllocation (exactly ten points above base 5), uses the same
SESSION/CSRF flow, and enforces three characters per account plus globally unique ASCII names.
Flyway V0005 adds character-owned tables and the transactional account guard; prior migrations remain unchanged.
See the [character backend report](docs/milestones/2b2-character-backend.md) for JSON examples and concurrency details.

Use the same Compose, backend `local` profile and Vite commands above. Existing VS Code launch settings and DBeaver connection details still apply. Milestone 2B.3 now provides React login/registration screens. The following manual API instructions are optional for developers using Postman; players use the browser UI. In DBeaver, accounts are in `steelstory.identity_accounts`; session tables are framework-owned and must not be edited by application modules.

Use one host consistently (`localhost` or `127.0.0.1`) so the cookie jar can retain the session. Postman's cookie jar must be enabled; no Authorization header is needed.

1. `GET http://localhost:8080/api/v1/auth/csrf` returns `{"headerName":"X-CSRF-TOKEN","token":"..."}` and sets an HttpOnly `SESSION` cookie. Copy the returned token into an `X-CSRF-TOKEN` request header. In Postman's post-response script you can use `pm.environment.set("csrfToken", pm.response.json().token)`, then set the header value to `{{csrfToken}}`. Keep the environment local; do not export session/CSRF secrets.
2. `POST /api/v1/auth/register`, with that header and `Content-Type: application/json`, creates the account (201). Use a synthetic local address and a disposable 15–128 character password:

   ```json
   {"email":"player@example.test","password":"local-only example passphrase"}
   ```

   Registration does not log in. A duplicate canonical email returns 409.
3. `POST /api/v1/auth/login`, with the same JSON, cookie and CSRF header, returns 200 and a safe account response. Postman accepts the rotated session cookie. Incorrect credentials and DISABLED accounts both return the same 401 response.
4. Fetch `GET /api/v1/auth/csrf` again and replace `csrfToken` after login. The pre-login token is invalidated.
5. `GET /api/v1/auth/me` returns the account from the session. It accepts no account selector; without a valid active session it returns 401.
6. `POST /api/v1/auth/logout`, with the refreshed CSRF header and an empty body, returns 204. `GET /api/v1/auth/me` then returns 401. Fetch a new CSRF token before another registration/login.

CSRF is required for **all three POST endpoints**, including anonymous registration, login and logout. Missing/invalid tokens return 403; invalid inputs return 400, and identity POST bodies over 4096 bytes return 413, including chunked requests. Error responses use `application/problem+json`. Never store sessions or credentials in localStorage.

Email addresses use a documented ASCII subset with a dotted domain, at most 64 characters before `@` and 254 overall after normalization. Surrounding whitespace is stripped and case normalized with `Locale.ROOT`; dots and plus tags are preserved. V0004 also enforces a single `@`, the allowed local-part characters/dot boundaries and nonempty DNS-style labels of at most 63 characters in PostgreSQL.

New passwords require 15–128 Unicode code points. Login accepts 1–128, so a correct older password below the current registration minimum still works. Both preserve spaces and reject malformed Unicode. At login, missing/null/empty passwords, excessive length, malformed Unicode and invalid email syntax return 400 before account lookup; valid input with an incorrect password, unknown email or DISABLED account returns the same 401. A wrong 1–14 character password therefore now returns 401 instead of the previous 400. CSRF 403 and request-body 413 limits are unchanged. See the [final review fixes](docs/milestones/2a-final-review-fixes.md).

The cookie is HttpOnly, host-only, SameSite=Lax and path `/`. `SESSION_COOKIE_SECURE` defaults to true, with false only in the local profile for HTTP development. `SESSION_IDLE_TIMEOUT` defaults to `12h`; production SPA/API must use the same HTTPS origin. Sessions and CSRF state persist in PostgreSQL across application restarts. An account disabled after login loses access on its next protected request.

**PUBLIC LAUNCH BLOCKER:** effective cross-instance controls for brute-force, credential stuffing and mass registration (including anonymous CSRF/session creation) are not implemented. Configure and verify admission/rate controls at the trusted ingress and/or a durable application mechanism, with monitoring and load testing, before public exposure. The duplicate-email 409 intentionally reveals registration status. Email verification, recovery/password reset, email changes, account deletion, MFA and administration remain future requirements. See [ADR 0003](docs/adr/0003-identity-sessions-and-security.md).

## Checks and builds

From `backend/`, with Docker running:

```sh
./mvnw --batch-mode --no-transfer-progress clean verify
```

This compiles Java 25, runs unit/architecture tests, packages the executable JAR, and runs PostgreSQL Testcontainers integration tests through Failsafe. Docker absence fails the build; tests are not silently skipped. Tests cover domain boundaries, authentication, CSRF, cookies, JDBC sessions across application restarts/instances, concurrent registration, empty/upgrade migrations, constraints and status/health. The exported contract is written to `backend/target/openapi.json` and compared structurally with the reviewed file. Update the checked contract deliberately with API changes.

Maven also runs the Identity/security Checkstyle gate during `validate` and rejects raw types during compilation. For a fast style-only check, run `./mvnw validate`. The checker does not rewrite source files. See the [quality-gate report](docs/milestones/2a-quality-gate.md) for the audit and verification on Temurin 25.0.5.

From `frontend/`:

```sh
npm ci
npm run typecheck
npm run lint
npm test
npx playwright install chromium
npm run test:ui
npm run build
```

Oxlint 1.93.0 supplies linting while preserving TypeScript 7 compatibility. Build output is `frontend/dist/`. The backend JAR is `backend/target/steelstory-0.1.0-SNAPSHOT.jar`; it can be run with `java -jar ... --spring.profiles.active=local` from the repository root using that path.

`npm run test:e2e` additionally runs the actual browser acceptance flow with Spring Boot and PostgreSQL already running. On Windows, `$env:PLAYWRIGHT_CHANNEL = 'msedge'` uses installed Edge instead of downloading Chromium. Tests create synthetic local accounts and characters and retain them because deletion is outside scope. GitHub Actions runs backend verification on a Docker-capable Linux runner and frontend installation/typecheck/lint/unit tests/UI tests/build. Actions are pinned to commit SHAs. The real-stack browser test is currently local, and there is no deployment job.

## Verification and limitations

Executed successfully: Maven Wrapper build/tests, frontend clean installation/typecheck/lint/build, Compose PostgreSQL startup, packaged backend startup, OpenAPI export, and a headless Edge browser check of connected/error/retry states through Vite. A temporary row and the single Flyway history entry survived backend restart plus PostgreSQL restart/container recreation; the probe table was removed afterward.

The paragraph above records Milestone 1 smoke checks. Milestone 2A verification and exact commands are recorded in the [implementation report](docs/milestones/2a-identity.md). Hosted GitHub Actions has not run. Milestone 2B.3 adds pinned Playwright development tooling and verifies the real registration/login/character flow in Edge, alongside responsive and failure-state tests. See the [frontend report](docs/milestones/2b3-character-frontend.md) and [character backend report](docs/milestones/2b2-character-backend.md). Further gameplay, payment integration and deployment remain deferred.

## Architecture documents

- [Architecture](docs/architecture.md): boundaries, dependencies, concurrency and scaling
- [Domain model](docs/domain-model.md): approved requirements and future aggregate rules
- [ADR 0001](docs/adr/0001-modular-monolith.md): modular monolith decisions
- [ADR 0002](docs/adr/0002-tech-stack.md): actual dependency/toolchain versions and compatibility evidence
- [ADR 0003](docs/adr/0003-identity-sessions-and-security.md): Identity security, JDBC sessions and public-launch blockers
- [Development guidelines](docs/development-guidelines.md)
- [Agent instructions](AGENTS.md)

Approved requirements include one-to-many Account -> Character cardinality, Strength/Dexterity/Intelligence/Constitution/Luck, offline asynchronous arena initiation and the planned auction house. Explicitly marked business proposals, including the wallet ownership split, remain outside this foundation's implementation scope.
