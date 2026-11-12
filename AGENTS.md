# Instructions for AI coding agents

## Current phase and scope

The Steelstory architecture is APPROVED. Milestone 1: Foundation is explicitly authorized: backend/frontend setup, PostgreSQL/Flyway, status/error handling, OpenAPI, tests and CI. This authorization permits dependency installation and relevant verification. Do not implement authentication, gameplay, currencies, PvP, inventory or marketplace, or scaffold empty domain modules. Stop after Milestone 1; do not start Milestone 2 automatically. Do not commit or push.

Read [README](README.md), [architecture](docs/architecture.md), [domain model](docs/domain-model.md), [development guidelines](docs/development-guidelines.md), and relevant [ADRs](docs/adr/0001-modular-monolith.md) before implementation. Treat proposed game rules as proposals; do not silently convert unresolved choices into requirements.

## Preserve review decisions

- Account -> Character is one-to-many. Any initial character cap is product policy, not a permanent unique account reference on characters.
- The initial attributes are Strength, Dexterity, Intelligence, Constitution and Luck. Formulas are undefined. Extend explicit types/schema/rules deliberately; do not introduce a generic EAV framework.
- Standard arena PvP supports attacks against eligible offline opponents without acceptance. Capture both participants' consistent combat snapshots at match initiation.
- An auction house is planned. Fixed-price listings may precede bidding, holds and auction closing; do not build later features prematurely.
- Account-owned premium gold and character-owned silver remain a proposal requiring approval. The associated `economy -> identity` dependency is conditional, not automatically authorized by the overall review. Preserve explicit ownership, authorization and account-wide concurrency implications.
- OpenAPI documentation uses springdoc 3.2.0. Integration tests compare the exported contract with `docs/api/openapi.json`. Update both deliberately when public behavior changes. TypeScript client generation is deferred; use the small typed adapter until generation demonstrates a benefit. Consult [ADR 0002](docs/adr/0002-tech-stack.md) for actual versions and verification.
- Introduce modules incrementally with authorized features, tests and migrations. Never scaffold the complete module map at once.

## Respect boundaries

- Implement domain-oriented modules under the architecture's dependency allowlist. Access another module only through its named API. Never import its internals, access its tables/repositories directly, or expose persistence entities across boundaries.
- Keep game rules independent of controllers, Spring APIs, and persistence implementations. Put transaction orchestration in application services and SQL/external integrations in adapters.
- Preserve Spring Modulith verification, acyclic dependencies and explicit allowed-dependency declarations. Do not disable checks, open modules, or add reflection/service-locator shortcuts to bypass them.
- Prefer synchronous APIs. Add events only for a concrete benefit with documented delivery, transaction and idempotency semantics. Do not turn atomic money/item/reward operations into eventual workflows casually.
- Avoid speculative abstractions, generic base services and shared gameplay dumping grounds. Do not introduce microservices, Kafka, Kubernetes or Redis without demonstrated need and an architectural decision.

## Java and Spring coding standard

These rules apply to every new implementation and refactoring. Practical examples and package conventions are in [development guidelines](docs/development-guidelines.md#java-and-spring-coding-conventions). Apply them within the requested scope; do not reformat unrelated code.

- Put every public class, interface, enum and record in its own Java file. Never nest request/response DTOs or other public API models inside controllers.
- Keep DTOs with their owning module and boundary: HTTP DTOs in `<module>.internal.web.dto`, module API values in `<module>.api.dto` when needed. Foundation HTTP DTOs belong in `bootstrap.web.dto`. No global DTO package; Java `public` does not grant cross-module access.
- Prefer records for simple immutable DTOs. Choose ordinary classes when behavior, lifecycle or persistence mapping calls for them; do not make every type or persistence entity a record.
- Keep controllers focused on HTTP. Business rules belong in domain objects/domain services, use-case orchestration in application services, and data access in module-owned repositories/adapters. Use constructor injection for required dependencies. A constant status response does not justify a service.
- Use concrete services when an interface has no real boundary or alternative implementation to serve. Add classes, packages and layers only for a clear responsibility.
- Preserve public HTTP behavior and OpenAPI during style refactoring. Keep stable operation IDs, required fields and meaningful constraints; avoid redundant documentation annotations. Do not update the contract snapshot merely to make a refactoring test pass.
- Use four-space indentation, blank lines between methods, multiline long annotations and imports instead of fully qualified names (except unavoidable name collisions). Prefer lines within 120 characters, readable wrapping and expanded method bodies.
- Name types, methods and variables after their responsibility. Avoid vague `Data`, `Handler`, `Helper` or `Manager` names unless meaningfully qualified.
- Prefer explicit control flow. Use streams, Optional, generics, patterns and Java 25 features only when they improve clarity or safety, not solely to shorten code. Keep domain code independent of Spring and persistence APIs.

## Protect game correctness

- Backend state is authoritative. Validate actor ownership, eligibility, amounts, deadlines and game rules on every mutation; never trust client balances, outcomes, prices, seeds or clocks.
- Currency and ownership changes need explicit transaction boundaries, constraints, lock/version handling, idempotency and permanent business-effect uniqueness. Follow the documented lock order.
- Use checked integer money and bounded deterministic combat. Persist timed work, snapshots, rules versions and retry identities; JVM locks and timers are not cross-instance guarantees.
- Change schema only through Flyway. Never enable production automatic schema updates, edit applied migrations, or reset data to hide a migration failure.
- Verify dependency compatibility against official sources. Follow ADR 0002 and managed BOMs; do not guess versions, add previews, or claim a build was verified when only documentation was checked.
- Never commit secrets or log credentials, session tokens, payment secrets or hidden RNG seeds.

## Work narrowly and verify

- Make the requested change with the smallest coherent scope. Avoid unrelated refactoring, formatting sweeps, renaming, dependency upgrades, and changes to unrelated files. Preserve the user's existing edits.
- Write meaningful tests for new or changed behavior and regression tests for bugs. Use real PostgreSQL/Testcontainers tests for database, transaction, concurrency and migration claims. Test relevant failures, retries and unauthorized access as well as success.
- Do not add trivial tests that mirror implementation or invent application tests for documentation-only edits. Run the relevant established checks and report exact results, including anything not run.
- Inspect the final diff for unintended changes. Do not commit, push, publish, deploy or contact third parties unless the task authorizes that action.

## Keep documentation honest

Update architecture, domain/API documentation, development instructions and README when behavior or conventions change. Add/update the appropriate ADR for consequential decisions and explain changes to module dependencies. Separate verified facts, proposed defaults and unresolved questions.

Before finishing, report changed files, resolved versions, commands executed, test results, known limitations and local run instructions. Run `backend/mvnw verify` (from backend; Docker required) and frontend `npm ci`, `npm run typecheck`, `npm run lint`, `npm run build` using the pinned Node toolchain. Never skip integration tests because Docker is unavailable; report the blocker. Respect the Milestone 1 stop.
