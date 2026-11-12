# ADR 0003: Identity authentication, JDBC sessions and security boundaries

- Date: 2026-11-12
- Status: Accepted for the explicitly authorized Milestone 2A
- Public launch: BLOCKED pending effective abuse controls and deployment hardening

## Context

The same-origin browser SPA needs account registration and server-authenticated identity before character features. PostgreSQL is already the durable store. The implementation must support another backend instance without sticky routing, preserve Foundation behavior and expose no gameplay modules. Authentication screens belong to 2C.

## Decision

Implement only `identity`, with `@ApplicationModule(allowedDependencies = {})` and named `identity::api`. Export AccountId and AccountAccess.isActive; missing/disabled returns false. It is a fresh eligibility read, not authorization for a caller-supplied ID, an account lock, or a guarantee about a future mutation. Future account guards must define transaction/lock semantics when needed. Identity has no character/economy/bootstrap dependency.

An immutable Account validates required values and timestamp ordering, creates ACTIVE registrations and exposes authentication eligibility. Domain values contain no Spring, security, JDBC or JSON dependencies. The application service coordinates use cases through a narrow account repository port and a password-hashing port. Concrete JDBC and Argon2 adapters implement those actual infrastructure boundaries.

### Registration and credential policy

Email is canonicalized using strip and Locale.ROOT lowercase, including the local part by deliberate application policy. No Gmail/provider-specific rewriting is performed. The supported subset is ASCII, unquoted local parts (up to 64 characters), a dotted DNS-style domain (labels up to 63 characters), total canonical length up to 254 and raw input up to 320. Internationalized/quoted addresses are not supported initially; expanding this policy needs deliberate canonicalization/uniqueness review.

New passwords contain 15–128 Unicode code points, with no trimming, Unicode normalization or composition rules. Login validates only safe input bounds (1–128 code points), independently of the new-password minimum, so future increases of that minimum do not lock out existing users. Both paths reject malformed UTF-16 so distinct malformed inputs cannot become identical replacement bytes. Identity POST request bodies are limited to 4096 bytes before JSON parsing, including chunked transfer. Separate registration and login DTO schemas describe their respective code-point bounds.

Use Spring Security's Argon2PasswordEncoder with Argon2id, version 19, 19 MiB memory, two iterations, parallelism one, a fresh random 16-byte salt and 32-byte output. These meet the [OWASP baseline](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html). Spring requires [Bouncy Castle](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html); bcprov-jdk18on 1.86 is explicitly pinned because Boot does not manage it here. This is the library implementation, not custom cryptography. Encoded hashes retain algorithm and parameters; future rehash policy is deferred.

Validate and hash before opening the short application-owned registration transaction. Account creation time is truncated to PostgreSQL's microsecond precision so registration and later reads agree. Insert with PostgreSQL's canonical-email UNIQUE constraint and ON CONFLICT (email) DO NOTHING. Concurrent registrations return one 201 and one 409, not two accounts or an incidental 500. There is no check-then-insert race, global lock or generic idempotency subsystem. A retry after a committed registration returns 409.

Unknown emails perform a dummy Argon2 verification; wrong passwords and DISABLED accounts use the same 401 response. This reduces a simple missing-account timing distinction, but does not promise constant-time end-to-end behavior. At login, missing/null/empty passwords, excessive length, malformed Unicode and invalid email syntax return 400 before account lookup, regardless of account existence/status. A nonempty, well-formed 1–14 character password now reaches verification: a correct legacy credential succeeds, while an incorrect credential returns 401 rather than the former 400. The requested explicit duplicate-registration 409 reveals whether an address is registered; this is an accepted API tradeoff requiring abuse controls.

### Session and CSRF lifecycle

Use Boot-managed Spring Security 7.1.2 and Spring Session JDBC 4.1.1 on the existing PostgreSQL database. Boot's [JDBC session starter](https://docs.spring.io/spring-boot/reference/web/spring-session.html) configures the repository and filter. Flyway V0003 adapts the [framework PostgreSQL schema](https://docs.spring.io/spring-session/reference/configuration/jdbc.html), verified against the resolved JAR. Automatic initialization is disabled. Spring Session's own epoch-millisecond columns remain BIGINT; account times are timestamptz.

The JSON controller authenticates through Spring's AuthenticationManager and identity's AuthenticationProvider. Its HTTP session adapter invokes ChangeSessionIdAuthenticationStrategy plus CsrfAuthenticationStrategy, then explicitly saves the SecurityContext. This follows [Spring's explicit persistence requirements](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html). The principal is only the account UUID string, credentials are null and authorities empty. Sessions contain the framework security context and CSRF state, not email, password/hash or domain aggregates.

Use an explicit GET /api/v1/auth/csrf endpoint backed by HttpSessionCsrfTokenRepository. The client retains the SESSION cookie and sends the returned (BREACH-masked) token in X-CSRF-TOKEN. Default XOR token handling remains enabled. Login clears the old token and rotates the session ID. Logout calls CsrfLogoutHandler and SecurityContextLogoutHandler, invalidating the JDBC session and clearing authentication. Fetch a fresh token after either operation, as required by [Spring's CSRF lifecycle](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html). The token endpoint and account responses are no-store.

Cookie defaults are host-only SESSION, path /, HttpOnly, SameSite=Lax and Secure. Only the local HTTP profile defaults Secure to false; the environment can override it. The idle timeout defaults to 12 hours via SESSION_IDLE_TIMEOUT. There is no remember-me, JWT, localStorage state or external provider. No CORS permission is configured; production requires HTTPS and the same SPA/API origin.

Protected requests re-read current account eligibility through identity::api. A missing/disabled account causes session invalidation and 401. ME also rechecks the account and derives its ID from the authenticated principal; query/body IDs cannot select another account. Public status and health remain available even with a stale disabled-account cookie.

Eligibility is checked at request admission, not under a lock throughout the request. An account disabled concurrently can finish already admitted work. Future sensitive mutations must define atomic account guard/status rules in their application transactions; the current read API makes no stronger promise. No disabling/admin HTTP operation is implemented.

### HTTP authorization and errors

Only the specified auth entry points, GET status, GET health and explicitly enabled local/test docs are public. Every other normal request requires authentication. Error dispatch is allowed so the container can preserve errors; it does not make normal unknown routes public. Form login, HTTP Basic, default logout and saved-request redirects are disabled.

CSRF covers register, login and logout, including anonymous sessions. Missing/invalid CSRF returns a sanitized 403 ProblemDetail. Missing authentication returns 401; malformed input 400; duplicate canonical email 409; oversized identity bodies 413. HTTP DTOs live inside identity.internal.web.dto, separately from module API and domain values. IdentityProblemResponse documents the flattened ProblemDetail representation; framework errors may omit the application-specific code.

Passwords, hashes and CSRF DTOs redact toString. Unexpected-error logs include only the exception type because JDBC/parser messages can contain rejected values. Operators must not enable bind-value, request-body, cookie or authentication tracing on real-user traffic. This intentionally reduces automatic diagnostic detail; add structured, explicitly safe diagnostics when a concrete need appears.

### Persistence ownership and deployment

V0001 is immutable. V0002 owns identity_accounts, canonical-email uniqueness, status/hash/timestamp checks and NOT NULL/length constraints. V0003 owns identity_sessions and its attribute table; indexes serve session ID, expiry cleanup and principal lookup. Framework JDBC code is the only runtime session-table adapter. Gameplay modules must not query either account or session tables.

Final review found that V0002 accepted multiple separators and empty domain labels, including foo@@example.com, foo@example..com and foo@.com, although EmailAddress rejected them. V0004 adds named checks for exactly one separator, the supported 1–64 character ASCII local part without edge/consecutive dots, and dotted DNS-style labels of 1–63 characters without edge hyphens. It retains canonicalization/uniqueness and the 254-character bound, and does not attempt full RFC email validation. V0001–V0003 remain unchanged.

V0004 validates existing rows and fails transactionally when an invalid value exists. It does not silently normalize, replace or delete account identities. Such a failure requires explicit data review/remediation before retry, without resetting the database or editing migration history. PostgreSQL documents the existing-row scan and locking of [ADD CHECK constraints](https://www.postgresql.org/docs/18/sql-altertable.html); schedule this migration appropriately for the actual table size. Regression tests demonstrate the old acceptance gap, failure with preserved data/history, and successful retry after explicitly correcting synthetic fixtures in an isolated database.

Spring Session uses its supported JDBC transaction behavior for session metadata; account registration has its own short application transaction. A session persistence failure must not be treated as successful login. No account/currency/game mutation is split into an eventual workflow.

JDBC session attributes use the framework's Java serialization. Limit database write access and backup access; serialized sessions are trusted data and may be incompatible across future dependency upgrades. Such upgrades need compatibility tests or planned session invalidation. Production migrations should use a separate role/release step; this milestone does not implement that deployment system.

## Alternatives and consequences

- In-memory sessions are simpler but tie authentication to one process. JDBC provides durability/shared access using existing infrastructure.
- JWT adds revocation and browser credential-storage decisions without a current benefit. Server sessions make logout/status checks explicit.
- Redis is unnecessary for this scope. External OAuth providers, roles and MFA remain deferred.
- A CSRF cookie flow is also viable, but an explicit endpoint keeps the chosen token lifecycle straightforward and the session cookie HttpOnly.
- Database uniqueness plus a short insert transaction resolves the registration race without distributed locks.
- Interfaces exist for the module contract and persistence/cryptography boundaries. Services and HTTP/session adapters remain concrete.

Each protected request currently reads account eligibility; measure database load before considering caching. A cache cannot silently weaken disabled-account revocation. Hash parameters meet the baseline but require capacity/latency measurements on production hardware.

## PUBLIC LAUNCH BLOCKER and future requirements

There is no effective distributed brute-force, credential-stuffing or mass-registration protection yet. Anonymous CSRF requests can allocate durable sessions; Argon2 attempts consume CPU/memory and registrations consume storage. Before public exposure, implement/test suitable ingress admission controls and durable per-account/network policies, trustworthy proxy-address handling, abuse monitoring and load limits. Do not replace these with a JVM-only counter or claim production readiness.

Production also needs HTTPS/ingress configuration, secrets/least-privilege roles, backup/restore checks and operational review. Account ownership is not email ownership: email verification and recovery policy remain outstanding. Password reset, email changes, account deletion, administration, MFA and OAuth are future requirements, not placeholder endpoints. No character, economy or authentication UI work is included.

## Verification

Real PostgreSQL tests cover empty and V0001-upgrade migrations, preserved existing data/checksum, constraints, concurrent registration, HTTP authentication and error behavior, fixation/CSRF rotation, logout, idle expiry, Secure cookie defaults, DISABLED revocation and session reuse across independent application contexts and context restarts. Domain tests cover input boundaries, Unicode, invariants and salted hashing. Architecture verification enforces identity's empty dependency list, pure domain and no outside imports of its internals. OpenAPI is exported and compared structurally; the existing status path/schema are unchanged. Exact results and limitations: [implementation report](../milestones/2a-identity.md).
