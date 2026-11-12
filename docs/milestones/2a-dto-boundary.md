# Milestone 2A REST DTO boundary refactoring

Date: 2026-11-12. Scope: the existing Identity and Foundation REST boundary only. Decision: [ADR 0004](../adr/0004-rest-dto-boundary.md).

## Result and changed files

Controllers accept JSON request DTOs and return typed ResponseEntity values. IdentityDtoMapper owns explicit input/output mappings; application commands are independent of HTTP models. IdentityService retains all account operations, domain validation and the hashing/transaction boundary. Authentication still passes through Spring Security and the existing session adapter.

The six HTTP records were renamed, retaining their packages and public OpenAPI schema names:

| Directory under backend/src/main/java/com/steelstory | Previous file | Current file |
| --- | --- | --- |
| bootstrap/web/dto | StatusResponse.java | StatusResponseDto.java |
| identity/internal/web/dto | AccountResponse.java | AccountResponseDto.java |
| identity/internal/web/dto | CredentialsRequest.java | CredentialsRequestDto.java |
| identity/internal/web/dto | LoginRequest.java | LoginRequestDto.java |
| identity/internal/web/dto | CsrfResponse.java | CsrfResponseDto.java |
| identity/internal/web/dto | IdentityProblemResponse.java | IdentityProblemResponseDto.java |

Other implementation/test files:

- backend/src/main/java/com/steelstory/bootstrap/web/StatusController.java
- backend/src/main/java/com/steelstory/identity/internal/web/AuthController.java
- backend/src/main/java/com/steelstory/identity/internal/web/IdentityDtoMapper.java (new)
- backend/src/main/java/com/steelstory/identity/internal/application/RegisterAccountCommand.java (new)
- backend/src/main/java/com/steelstory/identity/internal/application/AuthenticateAccountCommand.java (new)
- backend/src/main/java/com/steelstory/identity/internal/application/IdentityService.java
- backend/src/main/java/com/steelstory/identity/internal/infrastructure/AccountAuthenticationProvider.java
- backend/src/test/java/com/steelstory/ArchitectureTest.java
- backend/src/test/java/com/steelstory/identity/IdentityIT.java

Documentation: AGENTS.md, README.md, docs/architecture.md, docs/domain-model.md, docs/development-guidelines.md, docs/adr/0003-identity-sessions-and-security.md, new docs/adr/0004-rest-dto-boundary.md and this report.

Architecture regression checks enforce DTO naming, DTO independence from application/domain/persistence/module API values, application/domain independence from web, and mapped controller request/response signatures. Existing account serialization and HTTP/PostgreSQL tests exercise the renamed DTOs and new command input. Secret-bearing records continue to redact toString; new commands do likewise. Records and a small handwritten mapper make Lombok/mapping processors unnecessary here. No dependencies were added or upgraded.

## Executed commands and results

PowerShell, repository root unless another directory is stated:

```powershell
git status --short
java -version
node --version
npm --version
docker info --format '{{.ServerVersion}}'
Start-Process -FilePath 'C:/Program Files/Docker/Docker/Docker Desktop.exe' -WindowStyle Hidden
docker info --format '{{.ServerVersion}}'
npx --yes --package=npm@10.8.2 npm --version

cd backend
.\mvnw.cmd --batch-mode --no-transfer-progress validate
.\mvnw.cmd --batch-mode --no-transfer-progress clean verify
.\mvnw.cmd --batch-mode --no-transfer-progress dependency:tree "-DoutputFile=target/dto-dependencies.txt"
.\mvnw.cmd --version
docker compose version

cd ../frontend
npx --yes --package=npm@10.8.2 npm ci
npx --yes --package=npm@10.8.2 npm run typecheck
npx --yes --package=npm@10.8.2 npm run lint
npx --yes --package=npm@10.8.2 npm run build
npx --yes --package=npm@10.8.2 npm ls --depth=0

cd ..
git diff --check
```

The first Docker check found the daemon stopped; Docker Desktop was started successfully. The shell's default npm was 11.19.1, so all frontend verification explicitly selected the pinned npm 10.8.2 without changing global tools.

The initial Maven validate found two controller signatures over 120 characters. Those lines were wrapped before full verification. Final clean verify succeeded with zero Checkstyle violations, 46 unit/architecture tests and 16 real PostgreSQL integration tests; zero failures, errors or skips. Coverage includes registration transaction/rollback/concurrency, legacy/invalid credentials, session/CSRF rotation, disabled-account revocation, cross-instance/restart persistence, migration upgrade/failure/retry, status/errors and OpenAPI export. The migration-failure log is an expected rollback regression case, not a failed test.

The exported OpenAPI matched docs/api/openapi.json structurally without editing the snapshot. All frontend commands passed; npm ci reported zero vulnerabilities. git diff --check passed. No further application test run was needed after documentation-only changes.

## Resolved versions

Actual toolchain: Temurin 25.0.5+8, Maven 3.9.16, Node 24.22.2, npm 10.8.2, Docker Engine 29.0.1, Compose 2.40.3-desktop.1, Testcontainers PostgreSQL 18.6 at the existing pinned digest.

The exported Maven dependency tree confirms the unchanged Boot 4.1.2, Spring Framework 7.0.10, Modulith 2.1.2, Security 7.1.2, Session JDBC 4.1.1, springdoc 3.2.0, Flyway 12.4.0, PostgreSQL JDBC 42.7.14, Bouncy Castle 1.86, JUnit Jupiter 6.0.3, Mockito 5.23.0 and Testcontainers 2.0.5. The build used Maven Checkstyle Plugin 3.6.0 with Checkstyle 14.4.0.

npm ls confirms React/React DOM 19.3.1, TypeScript 7.0.2, Vite 8.3.6, plugin-react 6.2.1, Tailwind/@tailwindcss/vite 4.3.5, Oxlint 1.93.0, React/DOM types 19.3.0 and Node types 24.13.6. Manifests and lockfiles are unchanged.

## Local run and limitations

Use the [README local commands](../../README.md#run-locally): from the root run docker compose up -d --wait, from backend run .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local", and from frontend run npm run dev with the pinned Node/npm toolchain. The API remains on port 8080 and Vite on 5173. Existing cookie/CSRF/Postman instructions still apply.

Local Windows verification is complete. Hosted Linux CI and a separate manual browser session were not run for this Java boundary refactoring. JSON, errors, session behavior, database migrations, module dependencies and the reviewed API contract are unchanged. Existing public-launch limitations remain documented in ADR 0003. No Milestone 2B work, commits or pushes were performed. The pre-existing untracked 2b1 specification/plan files were left untouched.
