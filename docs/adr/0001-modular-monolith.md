# ADR 0001: Domain-oriented modular monolith

- Date: 2026-11-11
- Status: Accepted with review amendments; currency ownership proposal remains pending
- Review update: 2026-11-11; overall architecture APPROVED WITH CHANGES

## Context

Steelstory will have tightly related progression, inventory, currency, and multiplayer workflows maintained initially by one developer. A purchase or reward must not leave money and items in inconsistent states. Requirements will evolve; operational capacity and traffic have not yet been measured.

## Decision

Build one Spring Boot backend artifact and one PostgreSQL database. Organize backend code into domain modules, initially as packages within one Maven application. Use Spring Modulith verification and explicit dependency allowlists to enforce boundaries. Keep domain rules independent of Spring MVC and persistence adapters.

Introduce modules incrementally as authorized features need them. The planned module map is a boundary design, not a request to scaffold twelve modules or their tables at once. Architectural approval does not authorize implementation in the current documentation-only task.

Each module exposes a small named API and owns its tables. Other modules use immutable API values and identifiers, never internal classes, repositories, or persistence entities. Prefer direct synchronous calls. A workflow's application service owns its database transaction and can call several module APIs within that transaction. Each participant validates its own invariants.

Use events only for a demonstrated requirement for independent, eventually consistent reactions. Financial settlement, ownership transfer, and reward delivery stay synchronous and atomic. If reliable asynchronous reactions are added, persist their publications with the originating transaction and make consumers idempotent.

The [architecture](../architecture.md) defines the authoritative module dependency table. Spring Modulith can check cycles, access to module internals, and declared dependencies; these checks belong in CI. [Official verification reference](https://docs.spring.io/spring-modulith/reference/verification.html).

## First review amendments

The initial proposal was reviewed on 2026-11-11. The modular monolith, shared transactional database, Spring JDBC and enforced boundaries are retained. The following amendments supersede the corresponding initial assumptions without changing transaction, idempotency, concurrency or security guarantees:

- **Approved:** Account -> Character is one-to-many. The initial proposal's one-active-character default is not a permanent schema constraint; actual character caps remain product policy. Character owns the relationship; identity provides account eligibility and a guard for concurrent admission without depending on character.
- **Approved:** initial attributes are Strength, Dexterity, Intelligence, Constitution and Luck. Use explicit typed fields and deliberate schema/API/rules version changes for extensions; no generic EAV framework. Formulas remain undecided.
- **Approved:** standard arena attacks are asynchronous and can target offline opponents without acceptance. Both snapshots are captured consistently at initiation; the former acceptance-time default is superseded.
- **Approved roadmap:** an auction house is planned; fixed-price listings may come first. Add bidding, funded holds and auction closing later within the same module boundaries. Auction policy is still open.
- **Proposed, requires approval:** account-owned premium gold and character-owned silver replace the original both-currencies-per-character proposal. Economy retains wallet/hold/journal ownership and conditionally adds a direct `identity::api` dependency alongside `character::api`. No reverse dependency is introduced. Do not treat this conditional edge as permission to implement the proposal before approval.

Account gold would provide one shared premium balance across sibling characters and account-only purchases. It also requires account-wide spending locks/deduplication, explicit payer/beneficiary authorization and audit scope, and deletion/refund policies that preserve account funds when a character changes. An alternative is per-character gold with later migration/transfer policy, but that couples premium value to character lifecycle. The preferred split remains pending approval; see [architecture](../architecture.md#account-cardinality-and-proposed-currency-ownership) for consequences. Cross-character trading or gifting is not implicitly authorized.

OpenAPI documentation is a new requirement. Its tooling and optional typed-client generation are recorded in [ADR 0002](0002-tech-stack.md), not as an additional gameplay module.

## Alternatives considered

| Alternative | Assessment |
| --- | --- |
| Technical-layer monolith with shared repositories | Simple initially, but encourages cross-domain coupling and makes ownership hard to enforce |
| Maven artifact per domain immediately | Strong compile-time isolation, but creates build and configuration overhead before boundaries have stabilized; revisit if package enforcement proves insufficient |
| Microservices | Introduce distributed failure and consistency problems into ordinary money/item operations; not justified by team size or measured load |
| Event-driven integration for every operation | Obscures transaction outcomes and creates retry/reconciliation work without a demonstrated benefit |

## Consequences

One release coordinates all modules. Cross-module transactions deliberately couple workflows to a shared transactional database. The shared database is also an availability dependency. Module verification does not inspect arbitrary SQL or guarantee correct business behavior; review and integration tests remain necessary.

Package boundaries and separate persistence models require some mapping code. Keep interfaces only at real boundaries; avoid universal repository abstractions, service interfaces for every class, and a shared gameplay framework.

Multiple instances of the same artifact can share PostgreSQL once sessions and job coordination are durable. This does not require microservices. Extracting a module later would require a new ADR, replacement of cross-module ACID transactions, and explicit failure/reconciliation semantics; it is not a transparent deployment switch.

## Review triggers

Revisit only when measured contention, an independent release need, a distinct trust boundary, or sustained operating cost justifies a change. First consider query/index improvements, bounded work, and more instances of the existing backend.
