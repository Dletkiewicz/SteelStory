# Milestone 2A — final review fixes

Status: **READY TO COMMIT**, 2026-11-12. This status covers the two requested corrections, not public-launch readiness. No commit or push was performed.

## Password creation versus login input

PasswordPolicy now exposes separate validateNewPassword and validateLoginInput methods. Registration still requires 15–128 Unicode code points. Login accepts nonempty passwords of 1–128 code points independently of the new-password minimum. Both retain the raw UTF-16 upper bound, malformed-surrogate rejection and exact password preservation. Argon2id parameters, encoder implementation and encoded hash format did not change.

docker compose exec -T postgres psql -X -A -t `
    -U steelstory_local `
    -d steelstory `
    -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"

With valid CSRF and an in-limit request body, login semantics are:

| Input/result | HTTP behavior |
| --- | --- |
| Missing, null or empty password; over 128 code points; malformed Unicode; invalid email syntax | 400 before account lookup |
| Well-formed credentials with an incorrect password, unknown email or DISABLED account | Identical 401 INVALID_CREDENTIALS response |
| Correct older password below the current new-password minimum | Successful authentication, 200 |
| Missing/invalid CSRF; body over 4096 bytes | Existing 403 / 413 behavior retained |

The deliberate public change is that nonempty, well-formed 1–14 character passwords reach credential verification instead of failing registration policy with 400. An incorrect short password returns 401; a correct legacy password works. Unknown accounts still perform dummy Argon2 verification. This does not claim constant-time end-to-end responses.

LoginRequest documents minLength 1 and maxLength 128, while the existing CredentialsRequest remains the registration schema with minLength 15. The JSON field names, endpoint paths, operation IDs and response structures are unchanged. OpenAPI changed only the login operation description, its request schema reference and the added LoginRequest schema. The exported contract was compared structurally against the deliberately updated snapshot.

Regression tests seed an account with a real Argon2 hash for a shorter legacy password, verify successful HTTP login and unchanged stored hash, and reject that password for new registration. A matrix checks short/wrong passwords and malformed/oversized inputs for active, disabled and missing accounts, including raw JSON escapes for isolated UTF-16 surrogates. Domain tests cover one-character/space/supplementary-character passwords and upper bounds.

## Email database invariants

The real PostgreSQL regression test confirmed:

| Address | EmailAddress | V0002/V0003 database | With V0004 |
| --- | --- | --- | --- |
| foo@@example.com | Rejects | Accepted | Rejects |
| foo@example..com | Rejects | Accepted | Rejects |
| foo@.com | Rejects | Accepted | Rejects |

V0004 adds three named constraints: exactly one @, the supported ASCII local part (1–64 characters with no leading/trailing/consecutive dots), and a dotted domain with nonempty DNS-style labels (1–63 characters, no edge hyphens). Existing canonical-case, total-length, NOT NULL and uniqueness constraints remain. The checks describe the application's supported subset, not full RFC email syntax. EmailAddress itself required no change.

The migration validates existing values and fails if invalid rows exist; it does not delete, rewrite or normalize account identities. This follows PostgreSQL's [ADD CHECK validation behavior](https://www.postgresql.org/docs/18/sql-altertable.html). Migration testing demonstrates failure on the three invalid fixtures with all rows and the current V0003 history preserved, then successful retry after explicitly correcting only those synthetic fixtures in an isolated Testcontainers database. Tests also preserve prior migration checksums, verify 17 rejected email cases plus valid punctuation/plus tags and length boundaries, and reject an invalid direct SQL UPDATE.

Existing invalid data requires explicit review/remediation before V0004 can be applied. The migration scans and locks the table while adding constraints, so operational timing depends on the actual table size. No local Compose data was changed or reset. V0001, V0002 and V0003 are byte-identical to their pre-task versions.

## Verification

From backend, with Docker available:

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress clean verify
```

Final result: **BUILD SUCCESS**, **60 tests**, **0 failures**, **0 errors**, **0 skipped**, **0 Checkstyle violations**. Checkstyle runs in validate; Modulith, domain purity, security, real PostgreSQL migrations and exported OpenAPI comparison are part of the same successful lifecycle.

| Suite | Tests |
| --- | ---: |
| ArchitectureTest | 2 |
| ApiWebTest | 3 |
| IdentityDomainTest | 38 |
| Argon2PasswordHashingTest | 1 |
| FoundationIT | 3 |
| IdentityIT | 12 |
| IdentityMigrationIT | 1 |
| **Total** | **60 (44 unit/architecture + 16 integration)** |

All previous tests remain. FoundationIT's migration assertions now expect V0004 and four applied migrations; its contract check remains enabled. Final build log: `.tools/final-review-verify.log` (ignored local artifact); XML reports are in backend/target/surefire-reports and failsafe-reports.

From frontend, as required by repository verification instructions:

```powershell
npm exec --yes --package=npm@10.8.2 -- npm ci
npm exec --yes --package=npm@10.8.2 -- npm run typecheck
npm exec --yes --package=npm@10.8.2 -- npm run lint
npm exec --yes --package=npm@10.8.2 -- npm run build
```

All four commands passed; npm ci reported zero vulnerabilities. Frontend source and lockfile are unchanged. `git diff --check` passed; the final file comparison also checked new files for trailing whitespace and confirmed unchanged V0001–V0003, Argon2 adapter and dependency manifests.

Executed environment: Temurin **25.0.5+8**, Maven **3.9.16**, Node **24.22.2**, npm **10.8.2**, PostgreSQL **18.6** Testcontainers. Checkstyle **14.4.0** / Maven plugin **3.6.0** and all application dependencies remain unchanged. Hosted CI was not executed. Local run commands remain in [README](../../README.md#run-locally); normal startup applies V0004 through Flyway.

## Changed files

This list is relative to the working tree before these two fixes; earlier uncommitted milestone/quality-gate work remains intact.

```text
backend/src/main/java/com/steelstory/identity/internal/
    application/IdentityService.java
    domain/PasswordPolicy.java
    web/AuthController.java
    web/dto/LoginRequest.java                         (new)
backend/src/main/resources/db/migration/
    V0004__identity_email_invariants.sql               (new)
backend/src/test/java/com/steelstory/
    FoundationIT.java
    identity/IdentityIT.java
    identity/IdentityMigrationIT.java
    identity/internal/domain/IdentityDomainTest.java
docs/api/openapi.json
docs/adr/0003-identity-sessions-and-security.md
docs/domain-model.md
docs/development-guidelines.md
docs/milestones/2a-quality-gate.md                     (follow-up link)
docs/milestones/2a-final-review-fixes.md                (new)
README.md
```

No module architecture, authentication/session mechanism, dependency, game feature, Lombok or rate limiter was introduced. Milestone 2B has not started. The pre-existing public-launch abuse-control blocker remains outside this task; neither correction is blocked.
