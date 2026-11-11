# Development guidelines

Status: Updated after the first architectural review (APPROVED WITH CHANGES), 2026-11-11. These conventions govern future authorized implementation. Marked business/tooling proposals remain pending. The current task is documentation only.

## Work in small domain slices

Read [architecture](architecture.md), [domain model](domain-model.md), and the relevant ADR before changing code. Start from a player-visible use case and its invariants, identify the owning module, and add only the interfaces required by that use case. Do not create every proposed module, endpoint or table in advance.

Incremental module introduction is an explicit review requirement: create a module only with the feature that needs it, plus its meaningful boundary tests and migrations. A roadmap entry does not justify an empty module or speculative API. Auction bidding, holds and closing are later increments even if fixed-price trading is implemented first.

Preserve the approved one-to-many Account -> Character model. Enforce any initial cap as concurrency-safe product policy, not a permanently unique account reference on characters. Model Strength, Dexterity, Intelligence, Constitution and Luck explicitly; extend through typed changes, migrations and versioned snapshots rather than EAV. Standard arena attacks require no defender acceptance or online presence; capture both participants consistently at initiation. Keep account gold/character silver marked pending until approved.

Keep work scoped. Fix the requested behavior without unrelated renaming, formatting sweeps, dependency upgrades or refactoring. Explain necessary adjacent changes. Prefer plain language and explicit methods to generic frameworks. Use immutable value types and records where appropriate, composition over inheritance, and interfaces at actual module or infrastructure boundaries. Do not create an interface for every service merely to satisfy a pattern.

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

Use one Flyway migration sequence under the future `backend/src/main/resources/db/migration/`. Proposed convention: `V0001__identity_create_account.sql`, with repository-wide increasing numbers and the owning module in the description. Allocate numbers when preparing a change and resolve collisions before merge. Module ownership is enforced by review even though the migration runner can access every table.

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

OpenAPI documentation is required for implemented public endpoints. The proposed springdoc export workflow and verified Boot 4.1 candidate are in [ADR 0002](adr/0002-tech-stack.md#openapi-and-typed-client-tooling-amendment). Keep HTTP documentation annotations in web DTOs/adapters, not pure domain code. Describe stable operation IDs, schemas, session authentication and CSRF, actor/target semantics, idempotency headers, errors/conflicts, pagination, UTC dates and decimal-string currency/XP values. Exclude internal grants and wallet primitives from the public contract.

When this workflow is implemented, export a normalized contract from an integration test profile, validate it, compare it with the reviewed version, and test representative response serialization/security behavior. Review breaking changes before release; generated docs alone do not prove runtime conformance. Agree the OpenAPI dialect and supported schema subset with any client generator. Restrict or disable production docs endpoints/UI unless explicitly needed.

Typed TypeScript client generation remains a proposal. If adopted, pin the generator/configuration, keep generated files in a dedicated directory, never edit them by hand, and use a small hand-written wrapper for cookies/CSRF, errors and retries. Typecheck generated output with the actual frontend baseline and have CI detect regeneration drift. Keep contract documentation even if a hand-written client is chosen. Do not install tooling or create an endpoint spec before authorized implementation.

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

A change is ready when its scope is clear, relevant tests pass, boundaries remain enforced, migrations and retry behavior are reviewed where applicable, and documentation matches behavior. Explain any unrun verification or remaining blocker. Stop at an explicit user-requested review boundary; the present phase ends with architectural review, not scaffolding.
