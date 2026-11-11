# Steelstory

Steelstory is a planned asynchronous multiplayer browser RPG inspired by classic fantasy browser games. Characters choose a permanent Warrior, Archer, or Mage class, progress toward a planned level cap of 200, and participate in expeditions, automated combat, equipment progression, PvP, guilds, and a player marketplace. Silver is earned through gameplay; gold is premium currency.

**Status: first architectural review completed; APPROVED WITH CHANGES, 2026-11-11.** This revision records those changes. Explicitly marked proposals still require approval. This repository contains documentation only. There is no application, dependency installation, runnable build, database schema, Docker configuration, or CI workflow yet.

## Reviewed architecture

Use a Java 25 / Spring Boot 4.1 modular monolith, with Spring Modulith enforcing domain boundaries, a React/TypeScript browser client, and PostgreSQL as the authority for durable state. Synchronous module APIs and shared database transactions protect money, items, and rewards. Durable timestamps and database coordination allow the same backend to run on multiple instances later.

Approved clarifications: Account -> Character is one-to-many; an initial character limit is product policy. The five initial attributes are Strength, Dexterity, Intelligence, Constitution and Luck; formulas remain undefined. Standard arena PvP permits attacks against offline opponents without acceptance, with consistent snapshots at match initiation. An auction house is planned; fixed-price listings may come first, with bidding, holds and auction closing delivered later. Introduce modules incrementally as features need them.

**Pending proposal:** premium gold belongs to accounts and silver to characters. Economy remains the owner of both wallet implementations. OpenAPI contract documentation is required; springdoc 3.2.0 is the verified tooling candidate, and generating a typed TypeScript client is under consideration. See ADR 0002 for evidence and validation limits.

| Document | Purpose |
| --- | --- |
| [Architecture](docs/architecture.md) | Module responsibilities, dependency allowlist, transactions, security, risks, and scaling |
| [Domain model](docs/domain-model.md) | Vocabulary, ownership, aggregates, invariants, and example workflows |
| [ADR 0001](docs/adr/0001-modular-monolith.md) | Accepted architecture, review amendments and pending ownership proposal |
| [ADR 0002](docs/adr/0002-tech-stack.md) | Retained stack, version evidence, OpenAPI tooling proposal and validation limits |
| [Development guidelines](docs/development-guidelines.md) | Implementation, migrations, testing, and delivery conventions |
| [Agent instructions](AGENTS.md) | Rules for future AI coding work |

## Planned repository layout

After explicit authorization for implementation, application work can introduce `backend/` (one Maven application), `frontend/` (one Vite application), `infra/` (Docker Compose), and `.github/workflows/` (GitHub Actions). Add only the modules required by each authorized feature, not the whole planned module map. These directories are a plan, not existing functionality. There are intentionally no setup commands to run in this phase.

## Review decisions still needed

Approve or revise the gold/silver ownership proposal. Decide initial character limits, trading currency and auction policies, attribute/combat formulas and PvP effects, expedition policies, inventory overflow, premium refunds, API client generation/tooling, and operational targets. Cardinality, the five attribute names, asynchronous arena initiation and the planned auction house are approved requirements, not open questions.

This revision stops for documentation review. No gameplay or application scaffolding is implemented, and architectural approval does not authorize application work in this task.
