# ADR 0004: Strict REST DTO boundary

- Date: 2026-11-12
- Status: Accepted; explicitly requested as a Milestone 2A refactoring

## Context

REST transport models must evolve separately from domain aggregates and application use cases. Existing Identity endpoints already returned safe account records, but Java HTTP model names lacked the required Dto suffix and AccountResponse owned its domain conversion. Primitive service arguments also made the input use case boundary less explicit.

## Decision

All project-owned HTTP model names end in `Dto`. Keep them in the owning module's `internal.web.dto` or Foundation's `bootstrap.web.dto`. JSON request bodies use RequestDto types; controllers return `ResponseEntity<ResponseDto>`. A bodyless 204 uses `ResponseEntity<Void>`. Framework-injected servlet/authentication/CSRF arguments are not JSON request models.

Incoming flow is HTTP JSON -> request DTO -> explicit mapping -> application command/domain values -> application service -> domain behavior. Outgoing flow is application result/domain aggregate -> explicit web mapping -> response DTO -> HTTP JSON. Application/domain services never accept or return HTTP DTOs. Domain classes have no web dependencies. Controllers/mappers do not create aggregates, implement business operations or duplicate domain invariants.

IdentityDtoMapper is a small, handwritten, web-owned mapper. It constructs RegisterAccountCommand and projects Account into AccountResponseDto with only id, email, status and timestamps. Registration validation, hashing, aggregate creation and transaction orchestration stay in IdentityService/domain code. Hashing remains outside the short TransactionTemplate callback.

Login retains Spring Security's AuthenticationManager/AuthenticationProvider integration: the web mapper converts LoginRequestDto into an unauthenticated UsernamePasswordAuthenticationToken; AccountAuthenticationProvider converts the framework credentials into AuthenticateAccountCommand for IdentityService. SessionAuthentication keeps session rotation, CSRF invalidation and explicit SecurityContext persistence. No new authentication protocol or custom principal/token is introduced.

DTO records contain only transport fields, OpenAPI metadata and necessary secret redaction. Password-bearing application commands also redact toString. Spring ProblemDetail remains the framework transport representation for errors; IdentityProblemResponseDto documents its flattened wire shape. It is not a domain model.

Preserve existing public schema names with explicit type-level Schema names. Paths, operation IDs, statuses, required fields, constraints, JSON bodies and the reviewed docs/api/openapi.json remain unchanged. No schema migration or module dependency changes are required.

Architecture tests enforce DTO suffixes, transport-only DTO dependencies, application/domain independence from web, JSON request DTOs and typed response DTOs. Existing Modulith and domain-purity checks remain active. HTTP/PostgreSQL integration tests verify serialization, errors, authentication, CSRF/session behavior, transactions and contract compatibility.

## Alternatives and consequences

Handwritten mappings are explicit and small enough to review directly. A mapping framework would add processor/configuration overhead without simplifying these few fields; revisit only with demonstrated mapping complexity. The subsequent coding convention prohibits Lombok. Records already provide DTO/command state and accessors, so this change adds neither Lombok nor a mapping dependency.

Application commands add two purpose-specific input types without exporting new module APIs. Account may remain an internal application result, but it must be mapped before HTTP serialization. Constant technical responses such as status need no invented domain service or mapper. The subsequent Milestone 2B.2 authorization extends this boundary convention to character creation.

Verification, changed files, resolved versions and local run commands are recorded in the [refactoring report](../milestones/2a-dto-boundary.md).
