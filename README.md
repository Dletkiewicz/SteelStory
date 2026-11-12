# Steelstory

Steelstory is an asynchronous multiplayer browser RPG. The architecture is **approved**. Milestone 1 provides the technical foundation; no authentication or gameplay is implemented.

## Prerequisites

Use Java **25.0.2**, Maven **3.9.16** via the included Wrapper **3.3.4** (no global Maven required), Node **24.22.2** (see `.nvmrc`), npm **10.8.2**, and a running Docker daemon with Compose. Tested locally on Windows with Oracle JDK 25.0.2, Docker 29.0.1 and Compose 2.40.3. CI is configured for Linux/Temurin 25.0.2; hosted CI has not yet run.

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

Open [the frontend](http://127.0.0.1:5173). It displays the backend connection result and offers a retry button. Vite proxies `/api` to port 8080; production hosting must provide equivalent same-origin routing. `vite preview` serves the built bundle but does not provide the development API proxy.

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

- `GET http://localhost:8080/api/v1/status` returns `{"application":"steelstory","status":"UP"}`. This verifies API connectivity; it is not a database health report.
- `GET http://localhost:8080/actuator/health` checks application/database health without exposing details. Other Actuator endpoints are not exposed.
- `GET http://localhost:8080/v3/api-docs` exports OpenAPI 3.1 in the local profile. The reviewed contract is [docs/api/openapi.json](docs/api/openapi.json). No Swagger UI or generated client is installed.
- Unhandled failures return a sanitized RFC 9457 problem with `INTERNAL_ERROR`; framework errors preserve their HTTP status.

Flyway applies `V0001__foundation_create_schema.sql` at startup, creating only the `steelstory` namespace and its migration history in `public`. No domain tables exist. Applied migrations are immutable; startup validates checksums and fails on errors. SQL auto-initialization is disabled.

Without the local profile, `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` are required; docs are disabled. Optional `FLYWAY_USERNAME`/`FLYWAY_PASSWORD` allow a separately provisioned migration role. Local development uses one role. Production role provisioning, migration release orchestration, TLS/hosting and authentication remain future work; this is not a production deployment configuration.

## Checks and builds

From `backend/`, with Docker running:

```sh
./mvnw --batch-mode --no-transfer-progress clean verify
```

This compiles Java 25, runs four unit/architecture tests, packages the executable JAR, and runs two PostgreSQL Testcontainers integration tests through Failsafe. Docker absence fails the build; tests are not silently skipped. The tests verify context startup, JDBC, Flyway execution/revalidation, status/health HTTP responses and the OpenAPI contract. The exported contract is written to `backend/target/openapi.json` and compared structurally with the reviewed file. Update the checked contract deliberately with API changes.

From `frontend/`:

```sh
npm ci
npm run typecheck
npm run lint
npm run build
```

Oxlint 1.93.0 supplies linting while preserving TypeScript 7 compatibility. Build output is `frontend/dist/`. The backend JAR is `backend/target/steelstory-0.1.0-SNAPSHOT.jar`; it can be run with `java -jar ... --spring.profiles.active=local` from the repository root using that path.

GitHub Actions runs backend verification on a Docker-capable Linux runner and frontend installation/typecheck/lint/build. Actions are pinned to commit SHAs. There is no deployment job.

## Verification and limitations

Executed successfully: Maven Wrapper build/tests, frontend clean installation/typecheck/lint/build, Compose PostgreSQL startup, packaged backend startup, OpenAPI export, and a headless Edge browser check of connected/error/retry states through Vite. A temporary row and the single Flyway history entry survived backend restart plus PostgreSQL restart/container recreation; the probe table was removed afterward.

The six backend tests completed with zero failures/errors/skips. Hosted GitHub Actions has not run. Browser verification tooling was temporary and is not a project dependency. No gameplay, authentication, payment integration, sessions, domain modules, final game UI or automatic deployment is included. Stop at Milestone 1.

## Architecture documents

- [Architecture](docs/architecture.md): boundaries, dependencies, concurrency and scaling
- [Domain model](docs/domain-model.md): approved requirements and future aggregate rules
- [ADR 0001](docs/adr/0001-modular-monolith.md): modular monolith decisions
- [ADR 0002](docs/adr/0002-tech-stack.md): actual dependency/toolchain versions and compatibility evidence
- [Development guidelines](docs/development-guidelines.md)
- [Agent instructions](AGENTS.md)

Approved requirements include one-to-many Account -> Character cardinality, Strength/Dexterity/Intelligence/Constitution/Luck, offline asynchronous arena initiation and the planned auction house. Explicitly marked business proposals, including the wallet ownership split, remain outside this foundation's implementation scope.
