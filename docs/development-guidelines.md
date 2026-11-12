# Development guidelines

Status: Architecture APPROVED; Milestone 2A: Identity authorized, 2026-11-12. These conventions govern implementation. Marked business proposals remain pending; characters, gameplay and frontend authentication screens are outside this milestone.

## Work in small domain slices

Read [architecture](architecture.md), [domain model](domain-model.md), and the relevant ADR before changing code. Start from a player-visible use case and its invariants, identify the owning module, and add only the interfaces required by that use case. Do not create every proposed module, endpoint or table in advance.

Incremental module introduction is an explicit review requirement: create a module only with the feature that needs it, plus its meaningful boundary tests and migrations. A roadmap entry does not justify an empty module or speculative API. Auction bidding, holds and closing are later increments even if fixed-price trading is implemented first.

Preserve the approved one-to-many Account -> Character model. Enforce any initial cap as concurrency-safe product policy, not a permanently unique account reference on characters. Model Strength, Dexterity, Intelligence, Constitution and Luck explicitly; extend through typed changes, migrations and versioned snapshots rather than EAV. Standard arena attacks require no defender acceptance or online presence; capture both participants consistently at initiation. Keep account gold/character silver marked pending until approved.

Keep work scoped. Fix the requested behavior without unrelated renaming, formatting sweeps, dependency upgrades or refactoring. Explain necessary adjacent changes. Prefer plain language and explicit methods to generic frameworks. Use immutable value types and records where appropriate, composition over inheritance, and interfaces at actual module or infrastructure boundaries. Do not create an interface for every service merely to satisfy a pattern.

## Java and Spring coding conventions

These conventions are mandatory for new implementations and refactorings. Apply them to the task's scope rather than performing repository-wide cleanup. Use Java 25 features when they improve readability or safety; the existing no-preview policy still applies.

### Files, packages and DTOs

Every public class, interface, enum and record belongs in its own Java file named after the type. Request/response DTOs and other public API models must not be nested inside controllers. Non-public, narrowly scoped implementation details or test fixtures may remain nested when this genuinely helps understanding.

Keep models at their owning boundary:

| Kind | Package convention | Example |
| --- | --- | --- |
| Foundation HTTP DTO | `com.steelstory.bootstrap.web.dto` | `StatusResponse.java` |
| Domain module HTTP DTO | `com.steelstory.<module>.internal.web.dto` | A request/response used by that module's controller |
| Exported module API value | `com.steelstory.<module>.api.dto`, when a DTO subpackage is useful | An immutable command/result deliberately exposed through the named API |

HTTP visibility is different from module visibility. A public HTTP DTO in `internal.web.dto` is still internal to its Modulith module. Do not move it into the exported `api` package simply because a controller uses it. If module API DTOs need a subpackage, include it deliberately in the named interface and verify access with Modulith tests; package naming alone does not export it. Never introduce a global DTO collection or create empty packages in advance.

Prefer a record for a simple immutable request/response. Use an ordinary class when mutable state, lifecycle, identity or mapping requirements make it appropriate. Records are not an automatic choice for domain objects or persistence entities. A record containing a mutable collection still needs defensive copying when immutability is required.

`StatusResponse` is the reference example: a standalone record in `bootstrap.web.dto`, imported by `StatusController`. It retains the two required fields and their allowed values. The controller can directly construct this constant connectivity response; no service or interface is necessary.

### Responsibilities and dependencies

Controllers handle routing, HTTP input/output and delegation. Business rules belong in domain objects and domain services, while application services coordinate use cases and transactions. Module-owned repositories/adapters handle data access. This division preserves the existing pragmatic DDD boundaries; it does not require an extra service for every endpoint.

Use constructor injection for required dependencies, stored in final fields. Avoid field injection. For example, when a real use case needs a collaborator:

```java
private final ExpeditionService expeditionService;

public ExpeditionController(ExpeditionService expeditionService) {
    this.expeditionService = expeditionService;
}
```

This is a future-feature illustration, not authorization to create that module. A single constructor needs no redundant injection annotation. Prefer a concrete service when there is one implementation and no meaningful interface boundary. Retain interfaces for actual module contracts or persistence ports, not merely for mocking. Each added type, package or layer must have an identifiable responsibility.

### Formatting and naming

Use four spaces for indentation and blank lines between methods. Keep lines within 120 characters where practical; wrap long signatures, calls and annotations rather than compressing them. Put multiline annotation arguments on separate lines and keep method bodies expanded. Use imports instead of fully qualified class names in code, except when resolving a genuine name collision; prefer explicit imports over wildcards.

Choose names that explain purpose, such as `StatusResponse`, `ExpeditionService` and `findActiveExpedition`. Avoid unqualified names such as `Data`, `Helper` or `Manager`; a name like `ApiExceptionHandler` is appropriate when exception handling is its specific responsibility. Name variables for their meaning rather than shortening them unnecessarily.

Prefer straightforward conditionals and loops when they are easier to debug than chained streams. Use Optional for a meaningful absence contract, not as a replacement for every conditional. Introduce generics or design patterns only for an actual reusable relationship. Modern syntax is useful when the result is clearer, not merely shorter.

The 2A quality gate makes the following rules mandatory for Java production code and tests:

- Declare explicit local types; do not use `var` or raw types. Avoid a manual Optional presence check followed by `get()` when `orElseThrow` gives a clearer path. Do not add wrappers or generics without a concrete purpose.
- Name dependencies after their type/responsibility: `accountRepository`, `passwordHashing`, `transactionTemplate`, `securityContextRepository`, and `accountAuthenticationProvider` when disambiguation is needed. Short names such as `accounts`, `passwords`, `transactions` and `contexts` hide that responsibility. Business method names should state intent.
- Use four-space indentation, lines at most 120 characters in gated code, multiline long constructors/calls and a blank line between logical stages. After if/else or loop blocks, separate a distinct next stage (especially a return) with a blank line.
- Use explicit imports sorted lexicographically, followed by a blank line and sorted static imports. Import annotation types even in `package-info.java`; Java places those imports after the annotated package declaration.
- Use descriptive custom exception names ending in `Exception`, unless a documented alternative is justified. Keep shared stable messages/codes with their owner; reuse the exception's safe message constant at transport adapters where appropriate. Do not turn every string into a constant or introduce a global Errors/Constants bag.

For example, package annotations use normal imports:

```java
@NamedInterface("api")
package com.steelstory.identity.api;

import org.springframework.modulith.NamedInterface;
```

### Selective Lombok policy

Lombok is permitted when it removes meaningful repetitive work, not merely a few lines of correct constructors. Before adopting it, verify the chosen version with Java 25 and Maven, configure annotation processing explicitly, and run the build/tests. `@RequiredArgsConstructor` may replace a constructor that only assigns required final dependencies; `@Slf4j` may provide a logger in a class that logs. Keep constructors with validation or collaborator initialization explicit. Do not use `@Data`, generated domain setters or speculative `@Builder`; never replace behavior with generated accessors. A record remains preferable for a simple immutable DTO. No Lombok dependency or processor is installed in 2A.

### Automated Java quality checks

`backend/config/checkstyle/checkstyle.xml` runs through Maven Checkstyle Plugin 3.6.0 with Checkstyle 14.4.0, pinned separately from runtime dependencies. `./mvnw validate` checks Identity, bootstrap/security and their test packages; `./mvnw verify` includes the same gate. It checks four-space indentation, 120-character lines, braces/whitespace, explicit ordered/unused imports, naming patterns, exception suffixes, imported annotation types and the ban on `var`. The compiler uses `-Xlint:rawtypes -Werror` for production and test compilation.

This is a checker, not a formatter: it writes no source files. The scoped include list avoids imposing a Foundation formatting migration. The two existing architecture/contract tests received only explicit-type/import adjustments needed for the no-var rule. Future authorized module work must extend the gate's include list. Existing Foundation formatting outside the gate was not normalized.

Meaningful dependency names, logical-stage blank lines, exception-message safety and OOP responsibilities still need review; regex/name checks cannot establish semantic quality. Negative probes verified rejection of forbidden var, qualified annotations, bad exception names, imports, indentation, line length, basic names and raw generic types. No suppression was added.

### OpenAPI and refactoring compatibility

Keep stable `operationId`, required properties and important constraints such as the status response's allowed values. Avoid annotations that merely repeat information already inferred correctly. Long annotations should be formatted as in `StatusController` and `StatusResponse`; do not remove contract metadata to reduce visual clutter.

A style refactoring must preserve endpoint paths, HTTP methods/status codes, content types, JSON fields and generated OpenAPI schemas. Run existing HTTP tests and the integration contract comparison. Do not regenerate the checked contract to conceal a changed schema. Tests need editing only when they depend on the relocated Java type or when a behavioral coverage gap matters; do not add tests for file placement or record accessors.

## Backend boundaries

- Follow the module dependency allowlist in architecture. Import only another module's named API, never its internal class, JDBC adapter, repository, entity or table representation.
- Keep domain logic pure Java. Put transactions and orchestration in application services, SQL/external clients in infrastructure, and HTTP mapping in web adapters.
- Public module APIs exchange immutable commands/results and IDs. Do not return Spring Data types, lazy objects, mutable domain aggregates, JDBC rows or persistence entities.
- Repositories are module-owned ports describing needed persistence operations, not generic CRUD APIs exposed to the application.
- Keep explicit allowed dependencies and Modulith verification mandatory. Use additional architecture checks for domain purity and bootstrap wiring. Never work around a cycle with reflection, global bean lookup or an event bus.
- Dependency changes update architecture and affected ADRs. APIs should express business intent and document authorization, transaction participation, lock preconditions and retry behavior.

Use synchronous calls by default. Cross-module commands must share the caller's transaction when they form one business action. Durable events are appropriate only when independent eventual completion is acceptable, such as a future notification. At that point, record the publication in the originating transaction using Spring Modulith's persistent event-publication support, add idempotent consumers and test crash/retry recovery. Do not rely on an in-memory after-commit listener for guaranteed work or use events to split atomic rewards. Consumer dependencies still obey the module allowlist.

## Designing a sensitive mutation

Before implementation, specify these items in the change description or feature documentation:

1. Authenticated actor, target ownership, eligibility and trusted server-derived inputs.
2. Aggregate invariants and corresponding constraints/conditional updates.
3. Transaction boundary, lock set and acquisition order, including all recipients and refund participants.
4. HTTP retry identity and permanent business-effect identity; replay/conflict behavior and retention.
5. Failure behavior before/after commit and any worker retry path.
6. Tests for concurrent requests, duplicates, rollback and unauthorized targets.

Derive the account actor from authentication rather than trusting payloads, and verify ownership of the acting character. For arena, separately validate defender eligibility; do not require the attacker to own the defender. A scheduler has no browser principal; its application use case must explicitly define the authority to settle a persisted obligation. Keep minting, premium credit and reward APIs inaccessible to arbitrary HTTP input.

If the wallet proposal is approved, authorize gold against the account and silver against the character, also checking any character purchase beneficiary. Use explicit wallet owner scopes and the documented global lock order. Concurrent sibling-character gold spends share a wallet lock; account-scoped HTTP receipts include target IDs in their request hash, while payment/business deduplication survives character switching. Do not infer permission to transfer silver or trade gold from account ownership.

Use integer money, checked arithmetic and explicit maximums. Do not use `double` for currency. Use documented integer/fixed-point combat calculations with deterministic tie-breaking and bounded work. Inject time and random inputs into pure rules; do not scatter `now()` or random generation across entities.

Treat expected conflicts as ordinary domain outcomes. Retry deadlock/serialization failures only around an entire rollback-safe transaction, with a small bound and backoff. Do not retry non-idempotent external actions. Keep external calls out of database transactions and redact sensitive data from logs.

## Database changes

Use one Flyway migration sequence under `backend/src/main/resources/db/migration/`. Foundation V0001 is immutable; Identity adds V0002 (accounts) and V0003 (sessions). Subsequent authorized changes start at V0004, with repository-wide increasing numbers and the owning module in the description. Allocate numbers when preparing a change and resolve collisions before merge. Module ownership is enforced by review even though the migration runner can access every table.

Never edit an applied versioned migration; add a forward migration. Include constraints, indexes and relevant data backfills explicitly. Never use automatic production schema updates, implicit ORM DDL, or framework session/event schema creation. Keep runtime database permissions separate from migration permissions; production runtime has no DDL privileges.

Review locks and duration of schema changes. Use additive columns/tables first, compatible application changes, bounded backfills, then later contraction after old instances drain. Large index changes may require a PostgreSQL-specific nontransactional migration; justify and test those separately. Do not use Flyway `repair`, `clean`, or destructive reset as a routine production fix.

Test migrations on an empty PostgreSQL database and from the previous supported release schema. Backups need restore tests before production. Changes to retention/deletion must preserve outstanding listings, holds, rewards and financial provenance.

## Test meaningful behavior

| Layer | Purpose and tools |
| --- | --- |
| Pure domain tests | JUnit parameterized/boundary tests for rules, allocation, arithmetic, state transitions and deterministic combat; no Spring context |
| Application tests | Real domain objects plus Mockito for genuine boundary collaborators; assert outcomes and failure propagation, not incidental call ordering |
| Module tests | Spring Modulith module integration tests verify published API behavior and intentional collaborations |
| Architecture tests | Verify acyclic modules, named API access, declared dependencies, pure domains and restricted bootstrap access |
| Persistence/integration tests | Testcontainers PostgreSQL, real Flyway migrations, constraints, transactional rollback, lock behavior and idempotency |
| Security/API tests | Spring Security-enabled HTTP tests for authentication, CSRF, resource ownership, validation, payload limits and stable error contracts |
| Frontend tests | Typechecking, linting, component/user behavior and selected end-to-end flows once UI exists |

Do not use H2 as a substitute for PostgreSQL locking or SQL behavior. Use independent database connections/transactions and barriers/latches to coordinate concurrent tests. Assert one committed effect and correct final ledger/custody state; avoid arbitrary sleeps. Test rollback by injecting failure between actual participant writes. Tests relying on an outer test transaction must not mask commit-time failures or worker behavior.

Required sensitive-flow cases include double spend, sell/equip race, two buyers, repeated grant with matching/mismatched plan, concurrent bid/close, duplicate HTTP key, new key for the same business effect, crash/retry settlement, expired lease fencing if introduced, exact deadline boundaries, and level-cap/overflow limits. Implement relevant cases alongside each feature, not all tests before features exist.

Review-specific cases include concurrent character creation under a product cap without a one-to-one schema, offline arena initiation without acceptance, consistent snapshots during concurrent equip/allocation, and denied cross-account character access. Under the pending wallet proposal, also test sibling-character gold spending, cross-character receipt replay, wrong-account gold access, and gold preservation on character removal. Keep auction bid/close tests with the later auction features; fixed-price work does not require implementing auctions early.

Use fixed clocks and seeds, golden combat scenarios for each rules version, and invariant tests over a range of valid inputs. Retain old replay cases when rules change. Statistical balance analysis can supplement deterministic tests, but a flaky random pass/fail test is not a quality gate.

Avoid trivial tests that repeat implementation details or assert only getters/setters. Add regression tests for behavioral bugs. Documentation-only changes need document checks, not invented application tests. Run targeted checks during work and the established required suite before completion; report commands, results and limitations accurately.

## Frontend and HTTP contracts

Organize React features around player tasks, with a small shared UI layer and a central typed API adapter. Use strict TypeScript, explicit loading/error/conflict states, accessible controls and stable server contracts. Do not mirror backend business authority in a second frontend rules engine.

Keep money/large counters as decimal strings at the JSON boundary. Show server-provided outcomes and deadlines; client countdowns are estimates. For retried commands preserve the original idempotency key and payload. On conflict, refresh authoritative state rather than silently replaying with a new key. Never optimistically display a premium purchase or irreversible inventory transfer as completed before server confirmation.

Keep authentication cookies out of JavaScript, send CSRF tokens correctly, and treat all frontend configuration as public. Do not render untrusted HTML. Start with ordinary requests and polling; add transport/state libraries when they solve an existing problem.

### OpenAPI contract workflow

OpenAPI documentation is required for implemented public endpoints. Milestone 1 adopts springdoc 3.2.0 and an integration-test export compared structurally with `docs/api/openapi.json`; see [ADR 0002](adr/0002-tech-stack.md#milestone-1-implementation-record). Keep HTTP documentation annotations in web DTOs/adapters, not pure domain code. Document security, idempotency, errors, pagination, dates and amounts when those features exist. The current status endpoint is public and read-only. Exclude internal grants and wallet primitives from the public contract.

The current integration test exports and structurally compares the contract with the reviewed version and exercises real response serialization. Extend those checks with later API/security behavior. Review breaking changes before release; generated docs alone do not prove runtime conformance. Agree the supported schema subset with any future client generator. Runtime docs are disabled outside local/test configuration by default.

Typed TypeScript client generation is deferred until it demonstrates a benefit. The current adapter validates the status response at runtime. If generation is later adopted, pin the generator/configuration, keep generated files separate, never edit them by hand, and use a small wrapper for transport concerns. Typecheck generated output with the actual frontend baseline and detect regeneration drift in CI.

## Reproducible builds and CI

The version policy and checked evidence live in [ADR 0002](adr/0002-tech-stack.md). Use Java 25 without previews, the Maven Wrapper, Boot/Modulith BOMs, exact top-level frontend versions and a committed npm lockfile. Pin runtime images by digest for release and record the human-readable version. Do not use floating `latest` tags or ignore peer dependency failures.

When code exists, GitHub Actions should run on pull requests and the main branch with these gates:

1. Backend Maven `verify`: domain, architecture, security, module and PostgreSQL integration tests on a Docker-capable runner. Configure naming/lifecycle so integration tests actually run.
2. Frontend `npm ci`, typecheck, lint, behavior tests and production build using the pinned Node/npm toolchain. Verify the actual scripts before documenting commands.
3. Migration validation and empty/upgrade database tests; selected HTTP/browser smoke tests as implemented.
4. Dependency/security review and artifact packaging suitable to the changed scope; publish artifacts only through an authorized release workflow.
5. Once HTTP endpoints exist, export/validate and review the OpenAPI contract, test serialization and docs access policy, and check breaking changes. If client generation is selected, regenerate and typecheck it and reject unexplained drift.

Use least-privilege workflow permissions and reviewed action commit SHAs. Never expose deployment secrets to untrusted pull requests. Keep test data local and synthetic. Cache dependencies by lockfiles/toolchains; do not cache mutable database state. CI does not deploy automatically just because a build passed.

Docker Compose is the default local dependency environment; developers may run Java and Vite directly. Keep local credentials clearly disposable and out of production configuration. Record actual run commands only when tooling exists and has been exercised.

## Documentation and completion

Update domain rules, API behavior and operational instructions in the same change when affected. Record consequential decisions as ADRs with status, context, alternatives and consequences; preserve review amendments so a changed proposal is distinguishable from an approved requirement. Do not rewrite accepted history to conceal a reversal. Keep README truthful about what runs today.

A change is ready when its scope is clear, relevant tests pass, boundaries remain enforced, migrations and retry behavior are reviewed where applicable, and documentation matches behavior. Explain any unrun verification or remaining blocker. Stop after Milestone 2A; do not begin 2B, 2C or gameplay automatically.

## Identity security conventions

Follow [ADR 0003](adr/0003-identity-sessions-and-security.md). Derive the current account from the authenticated UUID principal. Bootstrap may call identity::api but cannot import internal services; standard Spring interfaces connect the provider/session adapters. An isActive read is not sufficient to serialize a future sensitive mutation.

Keep hashing outside registration's database transaction. The unique canonical-email constraint decides concurrency; do not replace it with a preflight SELECT. Spring Session SQL is owned by its JDBC adapter with Flyway migrations; do not access session tables from gameplay.

Keep `PasswordPolicy.validateNewPassword` separate from `validateLoginInput`: registration's minimum must not reject correct older credentials during login. Validate safe login input before account lookup; malformed input is 400 and well-formed incorrect credentials are uniformly 401. Preserve password bytes and Argon2 parameters. Extend database email checks only through new migrations; V0004 validates the supported ASCII subset without silently repairing existing account identities.

IdentityService retains TransactionTemplate because the transaction should begin after hashing and include only insertion/commit. Its default REQUIRED propagation joins an ambient transaction: it does not suspend a caller's transaction during hashing. The current HTTP registration caller is nontransactional, and registration is not an exported module API. Any future transactional caller must revisit that precondition. PostgreSQL tests observe transaction state at hashing/insertion and force failure after a real insert to verify rollback and safe retry.

Do not log request bodies, hashes, cookies, CSRF tokens or rejected database rows. Sensitive request/hash/CSRF values have redacted toString methods; unexpected-error logging records exception class only because driver/parser messages may contain data. Do not enable JDBC bind-parameter or HTTP body/header trace logging with real users. Authentication tests use synthetic data.

Document real flattened ProblemDetail JSON, explicit CSRF headers and cookie authentication. Hide framework-injected arguments from OpenAPI. Review the old endpoint/schema nodes before accepting a newly exported snapshot.
