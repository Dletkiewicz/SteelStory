# ADR 0005: Character creation and account serialization

- Date: 2026-11-12
- Status: Accepted for the explicitly authorized Milestone 2B.2 backend slice

## Decision

Introduce only the character module, with allowed dependency identity::api. It exports no named API yet. Character creation, list-own and get-own use immutable PlayerCharacter and explicit domain values. Class and ownership have no setters. StartingAllocation enforces ten nonnegative integer points; PlayerCharacter enforces base-5 starting attributes totaling 35 and valid progression/timestamps on both creation and restoration. Trained bonuses are separate from starting attributes and initially zero. There is no training, progression, equipment, respec or deletion operation.

The three-character cap is product policy in CharacterService. In a single READ COMMITTED transaction, insert a character-owned account guard if absent, SELECT it FOR UPDATE, check AccountAccess.isActive, count owned characters, then insert. The guard is keyed by AccountId, while the character owner column is non-unique. Concurrent first requests serialize through the guard's unique key; later requests serialize through its row lock. Errors roll back the whole transaction. All future operations that change slot usage must follow this guard protocol.

V0005 owns both tables. A generated canonical name uses PostgreSQL translate for exactly ASCII case folding and C collation for deterministic comparison. The UNIQUE constraint decides cross-account name races; targeted ON CONFLICT (canonical_name) DO NOTHING becomes a business conflict. Other database errors remain failures. CHECK constraints enforce name syntax, allowed classes, initial allocation, nonnegative state and timestamp ordering. No applied migration is edited.

The account reference deliberately has no foreign key to Identity's private tables. It references the character-owned guard instead. Active existence is checked through AccountAccess. Production module code never accesses Identity tables; an eventual account-deletion workflow requires a separately designed public API.

## Eligibility concurrency

AccountAccess.isActive is an eligibility read, not an Identity status lock. A disable concurrent with an already admitted creation may finish after that read. Character's guard serializes slot usage, not Identity status updates. This preserves the documented Identity contract and adds an eligibility read after acquiring the guard. A future atomic disable/delete guarantee requires an explicit Identity guard API and a joint lock protocol.

## HTTP contract

POST /api/v1/characters accepts name, characterClass and startingAllocation; returns 201 with Location. GET collection returns a characters envelope ordered by createdAt then ID. GET /{id} queries by both owner and ID; unknown and unowned IDs return 404. The owner is derived from the authenticated UUID principal. Client-supplied owner/progression properties are ignored and cannot affect persistence.

DTOs and explicit mappings live in character.internal.web. Allocation JSON is read losslessly as BigDecimal and converted with intValueExact so fractional/overflowing inputs cannot be truncated into valid points. The public schema describes integer allocation fields. Domain rules stay in domain values. Errors use the existing flattened ProblemDetail convention; authentication, JDBC sessions and CSRF use the existing security configuration.

Anonymous GET and POST with valid anonymous CSRF return 401. Missing/invalid CSRF on POST returns 403 before authentication, preserving the existing filter order. No new character-specific request-body limit is claimed; the Identity 4096-byte limit remains scoped to Identity.

## Scope and verification

No dependency additions, frontend changes, new authentication mechanism, generated client, gameplay module scaffolding or branch/commit/push. Checkstyle now covers character source/tests. Modulith verifies exactly identity and character, and domain/DTO architecture gates remain active. PostgreSQL tests exercise concurrent last-slot/first-guard/name races, ownership, security, initial persistence, migration upgrades and constraints. The OpenAPI update adds character operations/schemas while preserving all existing operation/schema nodes.

See the [implementation report](../milestones/2b2-character-backend.md) for exact results and commands.
