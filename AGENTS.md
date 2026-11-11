# Instructions for AI coding agents

## Current phase and scope

Steelstory's first architectural review is complete: APPROVED WITH CHANGES. The current deliverable is the documentation revision and consistency review only. Do not scaffold the application, implement gameplay, install dependencies, create build manifests, or add infrastructure until the user authorizes the next phase. Architectural approval alone does not lift this restriction. A later explicit implementation request lifts it for that request; it does not require repeated permission for ordinary authorized work.

Read [README](README.md), [architecture](docs/architecture.md), [domain model](docs/domain-model.md), [development guidelines](docs/development-guidelines.md), and relevant [ADRs](docs/adr/0001-modular-monolith.md) before implementation. Treat proposed game rules as proposals; do not silently convert unresolved choices into requirements.

## Preserve review decisions

- Account -> Character is one-to-many. Any initial character cap is product policy, not a permanent unique account reference on characters.
- The initial attributes are Strength, Dexterity, Intelligence, Constitution and Luck. Formulas are undefined. Extend explicit types/schema/rules deliberately; do not introduce a generic EAV framework.
- Standard arena PvP supports attacks against eligible offline opponents without acceptance. Capture both participants' consistent combat snapshots at match initiation.
- An auction house is planned. Fixed-price listings may precede bidding, holds and auction closing; do not build later features prematurely.
- Account-owned premium gold and character-owned silver remain a proposal requiring approval. The associated `economy -> identity` dependency is conditional, not automatically authorized by the overall review. Preserve explicit ownership, authorization and account-wide concurrency implications.
- OpenAPI documentation is required. The springdoc export workflow/tooling and generated TypeScript client remain proposals; consult [ADR 0002](docs/adr/0002-tech-stack.md) for source-verified compatibility and pending build checks.
- Introduce modules incrementally with authorized features, tests and migrations. Never scaffold the complete module map at once.

## Respect boundaries

- Implement domain-oriented modules under the architecture's dependency allowlist. Access another module only through its named API. Never import its internals, access its tables/repositories directly, or expose persistence entities across boundaries.
- Keep game rules independent of controllers, Spring APIs, and persistence implementations. Put transaction orchestration in application services and SQL/external integrations in adapters.
- Preserve Spring Modulith verification, acyclic dependencies and explicit allowed-dependency declarations. Do not disable checks, open modules, or add reflection/service-locator shortcuts to bypass them.
- Prefer synchronous APIs. Add events only for a concrete benefit with documented delivery, transaction and idempotency semantics. Do not turn atomic money/item/reward operations into eventual workflows casually.
- Avoid speculative abstractions, generic base services and shared gameplay dumping grounds. Do not introduce microservices, Kafka, Kubernetes or Redis without demonstrated need and an architectural decision.

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

Before finishing, summarize changed files, architectural consequences, remaining decisions, verification and material limitations. Respect explicit review stops. For this phase, update and internally review all seven documents for consistency, then stop for documentation review without implementing the application.
