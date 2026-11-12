# Milestone 2A — Java quality gate

Status: **READY FOR FINAL REVIEW** (2026-11-12). This is readiness to review the Identity quality refactoring, not approval for public launch or Milestone 2B.

Subsequent independent review requested two targeted behavior fixes; see [final review fixes](2a-final-review-fixes.md). The verification counts and unchanged-contract statements below describe this earlier quality-gate task.

The audit inspected the actual Identity classes, bootstrap security/session wiring, migrations, HTTP adapters, tests and architecture declarations. Changes below are relative to the completed 2A working tree at the start of this task, not the Foundation Git commit. Earlier uncommitted 2A changes were preserved. No commit, push, deployment, system-tool installation or database reset was performed.

## Findings by severity

| Severity | Finding | Resolution |
| --- | --- | --- |
| CRITICAL | No new critical defect identified within the inspected scope. | This is a code/test review, not a penetration-test certification. |
| HIGH | The existing public-launch blocker remains: no effective cross-instance admission controls for brute force, credential stuffing, mass registration or anonymous session creation. | Retained explicitly in README and ADR 0003. Requires a separate implementation and load/abuse verification before public exposure; this refactor adds no new authentication features. |
| MEDIUM | Existing tests covered concurrency and durable sessions but did not directly establish registration's hashing/transaction boundary or rollback after an insert. | Added two tests using the real PostgreSQL repository and transaction manager, with observing/failing adapters. |
| MEDIUM | The requested Java conventions had no automated enforcement. | Added a scoped Checkstyle validate gate and compiler rejection of raw types; verified both positive builds and negative probes. |
| LOW | Stable credential/duplicate-email/unauthenticated messages were repeated across adapters. | Reused safe exception-owned message constants and a single unauthenticated problem writer; retained exact public text and codes. |
| LOW | Local instructions implied a specific Oracle JDK installation was needed. | Documented compatible JDK 25 and verified the installed Temurin 25.0.5; preserved historical version records and CI's patch pin. |
| STYLE | Short dependency names, inferred local types, repeated Optional.get(), inconsistent imports/wrapping and exception names without the conventional suffix. | Applied the requested conventions within Identity/security and tests. These were readability issues, not evidence of broken authentication or invalid OOP. |

No new security defect requiring a behavior change was established. The existing duplicate-registration 409 deliberately reveals registration status; that reviewed contract remains unchanged. A future caller starting a transaction before registration could include hashing in its own transaction; this is a documented caller precondition, not a defect in the current nontransactional HTTP entrypoint.

## Refactoring and examples

Dependencies now describe their responsibilities (`accountRepository`, `passwordHashing`, `transactionTemplate`, `securityContextRepository`, `jdbcClient`). Locals use explicit types in production and test code. Imports are ordered and explicit, including imported Modulith annotations in both package-info files. Logical stages and long calls are separated without a whole-project formatting pass.

Before:

```java
private final AccountRepository accounts;
private final PasswordHashing passwords;
private final TransactionTemplate transactions;
```

After:

```java
private final AccountRepository accountRepository;
private final PasswordHashing passwordHashing;
private final TransactionTemplate transactionTemplate;
```

Previously, authenticate checked an Optional for absence, performed the dummy hash in that branch, then repeatedly called get(). It now resolves the account once while preserving dummy verification for an unknown address:

```java
Account account = accountRepository.findByEmail(emailAddress).orElseThrow(() -> {
    passwordHashing.performDummyVerification(password);
    return new InvalidCredentialsException();
});

boolean passwordMatches = passwordHashing.matches(password, account.passwordHash());
if (!passwordMatches || !account.canAuthenticate()) {
    throw new InvalidCredentialsException();
}

return account;
```

`EmailAlreadyRegistered`, `InvalidCredentials` and `InvalidIdentityInput` became `EmailAlreadyRegisteredException`, `InvalidCredentialsException` and `InvalidIdentityInputException`. Their transport mappings retain the previous status, JSON, code and message. Shared messages stay with their owning exception/value policy; there is no global constants collection. `SecurityProblemWriter.writeUnauthenticated` owns the shared 401 response used by both the entrypoint and inactive-account filter.

## Lombok decision

Lombok was not added. Records already cover simple values/DTOs, and removing a few assignment-only constructors does not justify another build processor. IdentityService and SessionAuthentication also initialize collaborators, while Account protects invariants; those constructors should remain explicit. There is no claim that a Lombok version was configured or build-tested here.

AGENTS.md and the development guidelines allow selective `@RequiredArgsConstructor`/`@Slf4j` only with a concrete benefit and prior Java 25/Maven compatibility verification and explicit processor configuration. They prohibit generated domain setters, `@Data` and speculative builders.

## TransactionTemplate assessment

The template is intentional and retained. Registration validates input, hashes the password, builds an immutable account, then opens a short insertion/commit transaction. Hashing is expensive and should not occupy a transaction/connection. Moving method-level `@Transactional` onto register would change that boundary.

The current HTTP caller has no surrounding transaction. Default REQUIRED propagation would join an ambient transaction; the template does not suspend one while earlier code hashes. Registration is not exposed through AccountAccess. A future transactional caller must revisit this precondition.

The unique canonical-email constraint and `INSERT ... ON CONFLICT (email) DO NOTHING` settle concurrent registration in PostgreSQL. No check-then-insert race or new atomicity gap was found. The transaction commits before the application method returns. Registration does not create an authenticated session, so there is no account/session atomic-write requirement in that use case.

New tests observe that hashing runs without an active transaction, insertion runs inside one, and the result is committed. A second test inserts a real row and then injects a failure: the row rolls back, and retrying registration succeeds. Existing concurrent-registration assertions still require one 201, one 409 and exactly one row.

## OOP, SOLID, DRY and KISS assessment

- Account remains immutable with private construction, explicit register/restore factories, temporal/null invariants and canAuthenticate behavior. No setters or hypothetical future operations were added.
- AccountId, EmailAddress and PasswordHash remain immutable values. Email normalization/validation and password policy remain independent of Spring, JDBC and HTTP. Safe credential representations remain intact.
- IdentityService orchestrates use cases; AuthController maps HTTP; SessionAuthentication coordinates framework session operations; JdbcAccountRepository owns SQL. Persistence objects do not cross the module API.
- AccountRepository and PasswordHashing are useful persistence/cryptography boundaries, demonstrated by the transaction tests. AccountAccess is the named cross-module API. No interface was introduced merely to mirror a concrete class.
- The existing package/dependency structure is coherent. Modulith still verifies the closed Identity module, its named API and acyclic allowed dependencies; ArchUnit verifies domain purity and forbids external imports of Identity internals.
- Shared error behavior was consolidated without a generic service hierarchy or global error framework. DTOs remain simple records; no business behavior was forced into transport models.

## Automated gate and resolved environment

Maven Checkstyle Plugin **3.6.0**, with explicitly selected Checkstyle **14.4.0**, runs during validate on `com/steelstory/identity/**/*.java` and `com/steelstory/bootstrap/security/**/*.java`, including test sources. It checks indentation, 120-character lines, braces/whitespace, ordered explicit/unused imports, naming patterns, imported annotations, direct RuntimeException subclass suffixes and forbidden var declarations. Compilation uses `-Xlint:rawtypes -Werror` for main and test sources.

The checker does not rewrite sources. Existing CI's Maven verify invokes it without a workflow change. Semantic dependency names, logical-stage separation and responsibility placement still require review. Foundation formatting is outside the Checkstyle include list; future authorized modules must extend that list. Two existing architecture/contract tests received only explicit-type/import changes to meet the no-var rule.

Official plugin/engine sources and compatibility rationale are linked in [ADR 0002](../adr/0002-tech-stack.md#milestone-2a-quality-gate-amendment-2026-11-12). Actual execution used **Temurin 25.0.5+8**, **Maven 3.9.16**, **Node 24.22.2**, **npm 10.8.2**, **Docker Engine 29.0.1** and **PostgreSQL 18.6** Testcontainers. No JAVA_HOME override to Oracle was needed. Java release 25 and CI's Temurin 25.0.2 pin were retained.

Runtime dependencies did not change: Boot **4.1.2**, Security **7.1.2**, Session **4.1.1**, Modulith **2.1.2**, springdoc **3.2.0** and Bouncy Castle **1.86** remain as resolved in the 2A implementation. Frontend manifests/lockfile are unchanged.

## Executed verification

From backend, using its existing Maven wrapper:

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress validate
.\mvnw.cmd --batch-mode --no-transfer-progress test
.\mvnw.cmd --batch-mode --no-transfer-progress clean verify
```

Final clean verify: **BUILD SUCCESS**, zero Checkstyle violations, **43 tests, zero failures/errors/skips**. Clean compilation also removes obsolete pre-rename class files.

| Suite | Executed tests |
| --- | ---: |
| ArchitectureTest (Modulith and ArchUnit) | 2 |
| ApiWebTest | 3 |
| IdentityDomainTest (including parameterized cases) | 23 |
| Argon2PasswordHashingTest | 1 |
| FoundationIT (PostgreSQL, health and exported OpenAPI) | 3 |
| IdentityIT (HTTP/security/sessions/concurrency/transactions) | 10 |
| IdentityMigrationIT (real PostgreSQL upgrade/constraints) | 1 |

The 29 unit/architecture cases and 14 integration tests retain all previous test methods, inputs and assertions, with only exception-name updates where needed; two integration tests were added. Reviewed security coverage includes password hashing, safe responses, uniform invalid-login failures, disabled accounts, CSRF on every mutation, cookie attributes, session/CSRF rotation, logout, session expiry, body limits and JDBC sessions shared across independent application contexts/restarts. These contexts run in one test JVM; this is not production multi-JVM/load verification.

Temporary negative probes intentionally made validate fail for var, qualified annotations, missing exception suffix, indentation, excessive line length, wildcard/unused imports and invalid basic names. A separate raw-generic probe passed style inspection and failed compilation with raw-type diagnostics and Werror. Both temporary Java probe files were removed before final clean verify; no check suppression was used.

From frontend, the installed shell's npm 11 was bypassed using the pinned npm without a global installation:

```powershell
npm exec --yes --package=npm@10.8.2 -- npm ci
npm exec --yes --package=npm@10.8.2 -- npm run typecheck
npm exec --yes --package=npm@10.8.2 -- npm run lint
npm exec --yes --package=npm@10.8.2 -- npm run build
```

All four commands passed. npm ci reported zero vulnerabilities; this is the installation audit result, not a security certification. `git diff --check` passed. File comparison against the pre-refactor snapshot confirmed byte-identical V0001/V0002/V0003, `docs/api/openapi.json` and frontend files. The integration test also compared the newly exported OpenAPI structurally against the unchanged contract. No endpoint, status, authentication/session setting, SQL migration or public API was added or changed by this task.

Local diagnostic artifacts are ignored: `.tools/quality-verify.log`, `.tools/quality-style-negative.log`, `.tools/quality-raw-negative.log`; Maven XML results are under backend/target/surefire-reports and failsafe-reports. Hosted Linux CI was not run.

## Changed files in this task

Paths below are relative to the repository; exception arrows denote renames, not deleted behavior. Other files visible in git status belong to the preceding 2A implementation.

```text
AGENTS.md
README.md
backend/pom.xml
backend/config/checkstyle/checkstyle.xml                         (new)
backend/src/main/java/com/steelstory/bootstrap/security/
    ActiveAccountFilter.java
    IdentityRequestLimitFilter.java
    SecurityConfiguration.java
    SecurityProblemWriter.java
backend/src/main/java/com/steelstory/identity/
    package-info.java
    api/package-info.java
    internal/application/IdentityService.java
    internal/application/EmailAlreadyRegistered.java -> EmailAlreadyRegisteredException.java
    internal/application/InvalidCredentials.java -> InvalidCredentialsException.java
    internal/domain/Account.java
    internal/domain/EmailAddress.java
    internal/domain/PasswordPolicy.java
    internal/domain/InvalidIdentityInput.java -> InvalidIdentityInputException.java
    internal/infrastructure/AccountAuthenticationProvider.java
    internal/infrastructure/Argon2PasswordHashing.java
    internal/infrastructure/JdbcAccountRepository.java
    internal/web/AuthController.java
    internal/web/IdentityExceptionHandler.java
    internal/web/SessionAuthentication.java
backend/src/test/java/com/steelstory/
    ArchitectureTest.java
    FoundationIT.java
    identity/IdentityIT.java
    identity/IdentityMigrationIT.java
    identity/internal/domain/IdentityDomainTest.java
    identity/internal/infrastructure/Argon2PasswordHashingTest.java
docs/adr/0002-tech-stack.md
docs/development-guidelines.md
docs/milestones/2a-identity.md
docs/milestones/2a-quality-gate.md                               (new)
```

## Remaining work and local run

The known abuse-control public-launch blocker remains. Hosted CI, production ingress/TLS, production provisioning and abuse/load testing still need their own verification. Future modules must extend the scoped gate; a checker cannot prove SOLID or security. No additional 2A refactoring blocker was found in this review. Milestone 2B has not begun.

For local execution, follow [README](../../README.md#run-locally): start Docker/Compose, run the backend wrapper with the local Spring profile and start the frontend with the pinned Node/npm. Use any compatible installed JDK 25; no Oracle-specific path is required. The documented alternative database port remains available when 5432 is occupied. This task used isolated Testcontainers databases and did not reset the local Compose volume.
