# ADR 0002: Technology stack and compatibility baseline

- Date and source-check date: 2026-11-11
- Status: Core stack retained after approval with changes; exact baseline and new tooling proposals remain subject to implementation validation
- Review update: 2026-11-11; OpenAPI documentation required, typed-client generation under consideration
- Verification level: official documentation, release records, and source metadata; **not a resolved or executed build**

## Context and decision

Use the requested Java/Spring and React stack with PostgreSQL, Flyway, Docker Compose and GitHub Actions. Select stable releases, keep Spring dependencies aligned through BOMs, and pin reproducible tooling when implementation is authorized. Do not substitute a preview release to satisfy a version requirement.

The tables below are a documented baseline, not a claim that every component is the newest release. Recheck patches and security advisories when scaffolding. No dependencies were installed and no build manifests were created during this review phase.

The first review explicitly retains Java 25, Spring Boot 4.1, Spring Modulith, PostgreSQL, Flyway, Spring JDBC, React and TypeScript. Other existing stack choices remain unchanged. The review adds OpenAPI contract documentation and evaluation of a generated TypeScript client. This amendment does not approve every pending business rule or authorize application implementation. Modules and their dependencies are introduced only when an authorized feature needs them.

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

Docker Compose will define local PostgreSQL and optional application containers using the Compose Specification and `docker compose` CLI. Select/pin a supported Docker/Compose toolchain during bootstrap and verify it with Testcontainers on the actual runner. GitHub Actions will execute reproducible builds on a Docker-capable Linux runner, with actions pinned to reviewed commit SHAs. No workflow, Docker installation, image pull or speculative hosting platform is part of this task. [Compose model](https://docs.docker.com/compose/intro/compose-application-model/).

## OpenAPI and typed-client tooling amendment

**Approved requirement:** maintain OpenAPI documentation for implemented public HTTP contracts. **Proposed workflow:** Spring MVC adapters and DTOs define the contract, springdoc exports it in integration tests, and a normalized specification is reviewed/versioned with API changes. Do not generate domain entities or expose internal module APIs. A contract-first workflow remains an alternative, but two independently maintained authoritative specifications would invite drift.

**Verified tooling candidate: `org.springdoc:springdoc-openapi-starter-webmvc-api:3.2.0`.** The official compatibility matrix maps Boot 4.1.x to springdoc 3.1.x–3.2.x. The tagged 3.2.0 POM directly uses Boot 4.1.2. Select the API-only starter for contract export; a documentation UI is optional. This is an explicitly versioned non-Boot-managed dependency. [Compatibility matrix](https://springdoc.org/), [tagged POM](https://raw.githubusercontent.com/springdoc/springdoc-openapi/v3.2.0/pom.xml), [stable release](https://github.com/springdoc/springdoc-openapi/releases/tag/v3.2.0).

**Under consideration:** OpenAPI Generator's `typescript-fetch` client generator, which produces a typed Fetch client from an OpenAPI document. Its official metadata labels it stable, although the help description still says beta; its feature table also records schema limitations. Treat it as a candidate and validate the actual contract rather than assuming complete dialect support. [Official generator documentation](https://openapi-generator.tech/docs/generators/typescript-fetch/).

The client generator does not load Spring Boot classes, so its Boot 4.1 compatibility boundary is the exported specification. Exact generator release, OpenAPI dialect, schema subset and adoption remain open. No generator version has been installed or certified against Steelstory's TypeScript baseline. Pin a stable release only after checking its release requirements and demonstrating generation/typechecking in the implementation phase. Prefer simple DTO schemas, explicit decimal strings and stable operation IDs; test nullability, enums, dates and error bodies.

If adopted, generate into a dedicated frontend directory from the reviewed specification, record generator version/configuration and reject regeneration drift in CI. Keep a small hand-written adapter for cookies, CSRF, idempotency headers and error mapping; never hand-edit generated files. A hand-written typed adapter remains viable if generation adds more maintenance than it removes. Contract docs are required regardless of that choice. Protect production docs endpoints/UI independently of API authorization.

## Compatibility validation required when implementation is authorized

Documentation verification establishes a credible baseline, not end-to-end certification. The first implementation phase must:

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

OpenAPI makes HTTP changes reviewable and can reduce duplicate TypeScript definitions. Generation adds a toolchain and contract-evolution obligation; it neither implements authorization nor guarantees runtime payload validity. Documentation/source compatibility was checked for this amendment; no dependency resolution, API export, client generation or application test was run.
