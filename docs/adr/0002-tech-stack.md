# ADR 0002: Technology stack and compatibility baseline

- Date and source-check date: 2026-11-11
- Status: Architecture APPROVED; Milestone 1 baseline resolved and exercised locally
- Review update: 2026-11-11; OpenAPI documentation required, typed-client generation under consideration
- Verification level: official source research plus executed Milestone 1 builds, database integration tests and browser smoke checks; hosted CI execution remains pending

## Context and decision

Use the requested Java/Spring and React stack with PostgreSQL, Flyway, Docker Compose and GitHub Actions. Select stable releases, keep Spring dependencies aligned through BOMs, and pin reproducible tooling when implementation is authorized. Do not substitute a preview release to satisfy a version requirement.

The tables below retain the original documented baseline, not a claim that every component is newest. The final implementation record below supersedes review-phase verification limits and records what was actually resolved. Recheck patches and security advisories during maintenance.

The review retains Java 25, Spring Boot 4.1, Spring Modulith, PostgreSQL, Flyway, Spring JDBC, React and TypeScript. Other existing stack choices remain unchanged. Final architectural approval explicitly authorizes Milestone 1: Foundation. Modules and their dependencies are introduced only when an authorized feature needs them.

## Backend and database

| Component | Proposed baseline | Evidence and compatibility decision |
| --- | --- | --- |
| Java | Java 25 LTS, no preview features | Java 25 is an LTS release; choose a maintained JDK distribution and pin its current 25.x patch/image at bootstrap. [Java support roadmap](https://www.oracle.com/java/technologies/java-se-support-roadmap.html) |
| Spring Boot | **4.1.2** | Official requirements include Java 25 in the supported Java 17–27 range and Maven 3.6.3+. [System requirements](https://docs.spring.io/spring-boot/system-requirements.html) |
| Spring Modulith | **2.1.2**, separate Modulith BOM | The release-tag POM explicitly declares Spring Boot **4.1.2** and Testcontainers **2.0.5**. [Tagged source](https://raw.githubusercontent.com/spring-projects/spring-modulith/2.1.2/pom.xml), [release](https://github.com/spring-projects/spring-modulith/releases/tag/2.1.2) |
| Maven | **3.9.16**, via Maven Wrapper | Maintained stable 3.9 release; exceeds Boot's minimum. Maven 4 is not required. [Release history](https://maven.apache.org/docs/history.html) |
| PostgreSQL | **18.6** | Supported PostgreSQL 18 line; Flyway documents verification against PostgreSQL 18. Pin the chosen image digest later. [PostgreSQL version policy](https://www.postgresql.org/support/versioning/), [Flyway PostgreSQL support](https://documentation.red-gate.com/fd/postgresql-database-277579325.html) |

The official Modulith reference appendix encountered during research still showed an older compatibility table despite a 2.1 documentation header. The tagged release POM above is the concrete evidence for this pairing; do not infer compatibility from matching minor numbers alone.

Use the Spring Boot parent/BOM for these managed versions. The values are an audit of Boot 4.1.2's dependency table, **not individual version overrides**. [Official managed coordinates](https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html).

| Managed dependency | Observed version | Usage |
| --- | --- | --- |
| Spring Security | 7.1.2 | Authentication, sessions, CSRF, authorization |
| Spring Session JDBC | 4.1.1 | Durable shared browser sessions |
| Flyway core and PostgreSQL database module | 12.4.0 | Include both `flyway-core` and `flyway-database-postgresql` |
| PostgreSQL JDBC driver | 42.7.14 | Blocking persistence |
| JUnit Jupiter | 6.0.3 | Tests; do not force JUnit 5 simply from older examples |
| Mockito | 5.23.0 | Application collaborator tests where valuable |
| Testcontainers | 2.0.5 | PostgreSQL integration tests; use 2.x artifact names such as `testcontainers-postgresql` and `testcontainers-junit-jupiter` |

Use Spring MVC, Spring JDBC and the Boot-managed JSON stack. Keep HTTP DTOs and persistence adapters outside domain logic. Prefer explicit JDBC operations for transaction and locking clarity; no ORM is required. If JPA is later justified, its mappings must stay internal and Flyway must remain authoritative. Do not independently override Spring Framework, Security, Jackson, JUnit or Testcontainers without documenting why and validating the resolved dependency graph.

Flyway's PostgreSQL documentation requires the database-specific module in addition to core. Framework schemas such as sessions must also be supplied through explicit migrations, with automatic initializers disabled. [Flyway PostgreSQL documentation](https://documentation.red-gate.com/fd/postgresql-database-277579325.html).

## Frontend and tools

| Component | Proposed baseline | Evidence |
| --- | --- | --- |
| React and React DOM | **19.3.1**, same version | Stable release record. [React release](https://github.com/react/react/releases/tag/v19.3.1) |
| TypeScript | **7.0.2** | Stable 7.0 announcement and patch release; use explicit strict checking. [Announcement](https://devblogs.microsoft.com/typescript/announcing-typescript-7-0/), [patch release](https://github.com/microsoft/typescript-go/releases/tag/typescript%2Fv7.0.2) |
| Vite | **8.3.6** | Stable release. [Release](https://github.com/vitejs/vite/releases/tag/v8.3.6) |
| React Vite plugin | **6.2.1** | Tagged metadata accepts Vite `^8.0.0` and Node `^20.19.0` or `>=22.12.0`; its development setup uses React 19.3.1. [Tagged package](https://raw.githubusercontent.com/vitejs/vite-plugin-react/plugin-react%406.2.1/packages/plugin-react/package.json) |
| Tailwind CSS and `@tailwindcss/vite` | **4.3.5**, same version | Tagged plugin peer range includes Vite 8. [Tagged package](https://raw.githubusercontent.com/tailwindlabs/tailwindcss/v4.3.5/packages/@tailwindcss-vite/package.json), [release](https://github.com/tailwindlabs/tailwindcss/releases/tag/v4.3.5) |
| Node.js | **24 LTS**, current security patch pinned at bootstrap | Supported LTS line satisfying the Vite/plugin engine range. [Node release status](https://nodejs.org/en/about/previous-releases), [Vite requirements](https://vite.dev/guide/) |
| Package manager | npm supplied with the selected Node toolchain | Record exact npm version, commit a lockfile, and use `npm ci` in CI |

Vite's official React/TypeScript template currently lists React 19.3.1, TypeScript 7.0.2, Vite 8.3.6 and plugin-react 6.2.1 together. This corroborates the proposed combination, but the template is a moving branch, not an installation lock or proof of our application's runtime behavior. [Official template](https://raw.githubusercontent.com/vitejs/vite/main/packages/create-vite/template-react-ts/package.json).

Use the Tailwind Vite integration and test the production CSS output. Vite transforms source; typechecking must run separately. The browser is a client of a versioned HTTP contract, so Java and frontend library versions do not require a shared release number. Agree supported browsers before selecting CSS/browser targets. [Tailwind Vite installation](https://tailwindcss.com/docs/installation/using-vite).

Docker Compose defines local PostgreSQL using the Compose Specification and `docker compose` CLI. The implementation record below gives the exercised Docker/Compose toolchain. GitHub Actions is configured for builds on a Docker-capable Linux runner, with actions pinned to reviewed commit SHAs. Automatic deployment and additional hosting infrastructure remain outside Milestone 1. [Compose model](https://docs.docker.com/compose/intro/compose-application-model/).

## OpenAPI and typed-client tooling amendment

**Approved requirement:** maintain OpenAPI documentation for implemented public HTTP contracts. **Workflow adopted in Milestone 1:** Spring MVC adapters and DTOs define the contract, springdoc exports it in integration tests, and the specification is reviewed/versioned with API changes. Do not generate domain entities or expose internal module APIs. A contract-first workflow was considered, but two independently maintained authoritative specifications would invite drift.

**Verified tooling candidate: `org.springdoc:springdoc-openapi-starter-webmvc-api:3.2.0`.** The official compatibility matrix maps Boot 4.1.x to springdoc 3.1.x–3.2.x. The tagged 3.2.0 POM directly uses Boot 4.1.2. Select the API-only starter for contract export; a documentation UI is optional. This is an explicitly versioned non-Boot-managed dependency. [Compatibility matrix](https://springdoc.org/), [tagged POM](https://raw.githubusercontent.com/springdoc/springdoc-openapi/v3.2.0/pom.xml), [stable release](https://github.com/springdoc/springdoc-openapi/releases/tag/v3.2.0).

**Under consideration:** OpenAPI Generator's `typescript-fetch` client generator, which produces a typed Fetch client from an OpenAPI document. Its official metadata labels it stable, although the help description still says beta; its feature table also records schema limitations. Treat it as a candidate and validate the actual contract rather than assuming complete dialect support. [Official generator documentation](https://openapi-generator.tech/docs/generators/typescript-fetch/).

The client generator does not load Spring Boot classes, so its Boot 4.1 compatibility boundary is the exported specification. Exact generator release, OpenAPI dialect, schema subset and adoption remain open. No generator version has been installed or certified against Steelstory's TypeScript baseline. Pin a stable release only after checking its release requirements and demonstrating generation/typechecking in the implementation phase. Prefer simple DTO schemas, explicit decimal strings and stable operation IDs; test nullability, enums, dates and error bodies.

If adopted, generate into a dedicated frontend directory from the reviewed specification, record generator version/configuration and reject regeneration drift in CI. Keep a small hand-written adapter for cookies, CSRF, idempotency headers and error mapping; never hand-edit generated files. A hand-written typed adapter remains viable if generation adds more maintenance than it removes. Contract docs are required regardless of that choice. Protect production docs endpoints/UI independently of API authorization.

## Compatibility validation required when implementation is authorized

The original documentation verification established a baseline, not end-to-end certification. The checklist below covers foundation and later authorized features; the implementation record reports completed checks. Authentication/session/security integration and client generation remain deferred under the explicit Milestone 1 scope.

1. Resolve the Boot parent and Modulith BOM from stable repositories; inspect the effective POM and dependency tree for unwanted overrides or preview transitive versions.
2. Compile and test on the pinned Java 25 toolchain, including Modulith verification. Verify Mockito instrumentation on Java 25 using its supported test-agent setup if required; do not hide instrumentation failures with global JVM flags.
3. Start PostgreSQL 18 with Testcontainers, apply Flyway migrations, and test JDBC transactions and session persistence. Verify upgrade migrations as well as an empty database.
4. Resolve exact frontend package versions without ignoring peer errors, commit the lockfile, typecheck, and build the React/Tailwind production bundle on the chosen Node version.
5. Pin JDK/Node patches, npm, Docker images and CI action SHAs; record actual resolved versions. Run a browser/API smoke test when those surfaces exist.
6. Resolve the selected springdoc starter with Boot 4.1.2/Java 25, start a Spring MVC/Security integration context and export/validate the OpenAPI specification. Verify real JSON serialization, security/CSRF documentation, idempotency headers, conflict schemas and decimal-string amounts. Review contract changes and test docs endpoint access policy.
7. If typed-client generation is adopted, pin its stable release and dialect/configuration, generate from that exact contract and typecheck/build on the chosen TypeScript/Node baseline. Test cookies, CSRF, nullable fields, enum changes, error handling and idempotent retries; enforce reproducible output. Published compatibility evidence does not replace these checks.

If evidence or resolution conflicts, document the conflict and resolve it within the requested stable lines. Do not silently downgrade Java 25 or Boot 4.1, add snapshots, or claim validation that was not run.

## Consequences

The stack fits the required architecture with one relational transaction boundary and standard JVM tooling. Explicit SQL and separate domain values cost some mapping code but make ownership and concurrency visible. Spring Boot 4-era APIs, JUnit 6 and Testcontainers 2 mean older tutorials may use incompatible imports or artifact names. BOM management reduces manual version drift; periodic reviewed patch updates remain necessary.

OpenAPI makes HTTP changes reviewable and can reduce duplicate TypeScript definitions. Generation adds a toolchain and contract-evolution obligation; it neither implements authorization nor guarantees runtime payload validity. The original review used source evidence only; the implementation record below reports subsequent executed verification.

## Milestone 1 implementation record

Milestone 1 adopts the baseline and springdoc 3.2.0 API-only starter. Maven resolved Boot 4.1.2, Modulith 2.1.2, Spring Framework 7.0.10, Spring JDBC 7.0.10, Flyway 12.4.0, PostgreSQL JDBC 42.7.14, JUnit Jupiter 6.0.3, Mockito 5.23.0 and Testcontainers 2.0.5. Security/session libraries are not enabled in this milestone. No JPA was added.

The executed local toolchain is Oracle JDK 25.0.2, Maven Wrapper 3.3.4 downloading Maven 3.9.16, Node 24.22.2 and npm 10.8.2 on Windows. `.nvmrc` pins Node; package manifests and lockfile pin frontend dependencies. CI uses Temurin 25.0.2 on Linux; that runner/distribution has not been executed from this workspace.

Frontend resolution retained React/React DOM 19.3.1, TypeScript 7.0.2, Vite 8.3.6, plugin-react 6.2.1, Tailwind/plugin 4.3.5. Additional tooling is Oxlint 1.93.0, React/DOM types 19.3.0 and Node types 24.13.6. Registry inspection found typescript-eslint 8.72.2 requires TypeScript below 6.1, so Oxlint was selected to retain TypeScript 7 without ignoring peer requirements. Typecheck, lint and production build all execute successfully; no approved technology was downgraded.

The npm lockfile also records upstream optional WASM dependencies with prerelease versions (`@emnapi/core` and `@emnapi/runtime` 2.0.0-beta.2). These are transitive development-tool dependencies, not selected application libraries; no compatibility override was forced. The inspected installed Windows dependency graph reports their stable 1.11.3 variants. The Linux CI graph/build must still be exercised before claiming cross-platform verification.

Docker Engine 29.0.1 / Compose 2.40.3 ran PostgreSQL 18.6. Compose and integration tests pin its inspected official digest `sha256:e3d24f6906d0760bf4da0dae872415391ededefad23fd471c47dc53b0ddf3c42`. PostgreSQL 18 stores its versioned data directory under the mounted `/var/lib/postgresql` volume. Local port 5432 was occupied, so verification used the supported `POSTGRES_PORT=55432` / `DB_URL` overrides.

OpenAPI 3.1 is exported by the running application and compared structurally to `docs/api/openapi.json` by the PostgreSQL integration test. Fixed relative server metadata avoids random-port drift. Contract/DTO annotations are confined to bootstrap web adapters. Docs are enabled only in local/test configuration by default. Code generation is deferred because a single status endpoint is adequately served by a typed/runtime-checked adapter.

The full Maven lifecycle runs four unit/architecture tests and two real PostgreSQL integration tests with zero skips. Failsafe binds both integration-test and verify and fails when no tests are found; Docker absence is a failure, not an automatic skip. The test JVM uses Mockito's managed Java agent explicitly for Java 25. CI performs the same verification, frontend checks/build and confirms the integration report exists. No deployment workflow is present.

Production deployment, authentication/security/session setup, migration role provisioning and game modules remain future work. Default configuration requires environment database credentials; only the explicitly selected local profile/Compose file contains disposable development credentials. Flyway supports separate `FLYWAY_USERNAME`/`FLYWAY_PASSWORD`; local development intentionally uses one role. A production migration release process is not claimed by this foundation.

## Milestone 2A compatibility amendment (2026-11-12)

The historical Foundation record above remains unchanged. Identity now enables Boot-managed `spring-boot-starter-security` and `spring-boot-starter-session-jdbc` 4.1.2, resolving Spring Security **7.1.2** and Spring Session JDBC/core **4.1.1**. Existing Boot, Modulith, JDBC, Flyway, PostgreSQL, springdoc and frontend versions are retained. The Maven dependency tree and real HTTP/PostgreSQL tests verify the combination, including default cookie attributes, CSRF rotation and JDBC session reuse after context restart.

Spring's Argon2PasswordEncoder requires Bouncy Castle. Boot 4.1.2 does not manage `bcprov-jdk18on` in this project's effective dependency management, so **1.86** is pinned explicitly, following the [official stable download](https://www.bouncycastle.org/download/bouncy-castle-java/) and [Spring password-storage documentation](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html). Only bcprov is added; no provider installation or custom cryptography is required. Password hashing uses Argon2id with 19 MiB, two iterations and parallelism one. See [ADR 0003](0003-identity-sessions-and-security.md) for policy and limits.

Session setup follows [Boot's starter/autoconfiguration documentation](https://docs.spring.io/spring-boot/reference/web/spring-session.html) and [Session JDBC's PostgreSQL schema](https://docs.spring.io/spring-session/reference/configuration/jdbc.html), also inspected inside the resolved 4.1.1 JAR. V0003 changes only schema/table/index naming from the framework layout; timestamp columns used internally by Spring Session remain epoch-millisecond BIGINTs. Account timestamps use timestamptz.

Verification uses the repository baseline Oracle Java **25.0.2**, Maven **3.9.16**, Node **24.22.2**, npm **10.8.2**, Docker **29.0.1** and real PostgreSQL **18.6**. The shell initially selected Temurin 25.0.5 and npm 11.19.1; commands explicitly selected the pinned JDK and invoked npm 10.8.2 without changing global tools. Exact commands/results are in the [2A implementation report](../milestones/2a-identity.md). Hosted Linux CI and public production deployment remain unverified.

## Milestone 2A quality gate amendment (2026-11-12)

Java 25 is the language/runtime requirement; the historical Oracle 25.0.2 path and patch are not application requirements. The quality refactoring uses the developer's existing Temurin **25.0.5**, with no JDK installation, Java release change or CI patch change. Maven Wrapper and all runtime/frontend dependency versions remain unchanged. Historical verification records above retain the versions actually exercised.

Add Maven Checkstyle Plugin **3.6.0** and explicitly override its bundled engine with Checkstyle **14.4.0**, scoped to Identity/security source and tests. Official [Maven usage](https://maven.apache.org/plugins/maven-checkstyle-plugin/usage.html) and [engine override instructions](https://maven.apache.org/plugins/maven-checkstyle-plugin/examples/upgrading-checkstyle.html) support this setup. The [Checkstyle release](https://checkstyle.org/) and [IllegalType var rule](https://checkstyle.org/checks/coding/illegaltype.html) were checked; Maven execution on the actual Java 25 code verifies compatibility. The engine understands the records, text blocks and package annotations in use. No autoformat step or system tool is installed.

The quality gate runs in validate, so existing CI's Maven verify already invokes it. Compiler diagnostics `-Xlint:rawtypes -Werror` reject raw types in main/test code. Negative probes check that the gate fails on actual violations. Lombok remains absent because the current constructors do not justify an additional processor. Exact audit findings, checks and limitations are recorded in the [quality-gate report](../milestones/2a-quality-gate.md).
