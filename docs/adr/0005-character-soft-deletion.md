# ADR 0005: Owner-confirmed character soft deletion

- Date: 2026-11-13
- Status: Accepted under the explicit character deletion and class portraits request

## Context

The player needs to free one of three character slots during development and
gameplay testing. The previous email-confirmation proposal is deferred; the user
explicitly authorized an in-app confirmation flow without email delivery.

## Decision

Add owner-only `DELETE /api/v1/characters/{id}` using existing authenticated
sessions and CSRF. The controller derives the actor from the principal and calls
`CharacterService.deleteOwn`; it returns `ResponseEntity<Void>` with 204. Unknown,
unowned or already deleted targets return the same 404. Missing authentication is
401 when a valid CSRF token is supplied; missing/invalid CSRF can be rejected with
403 before authentication, as for existing mutations. Malformed IDs return 400.

The transaction acquires the character-owned account guard, checks account
eligibility through `identity::api`, loads the active owned target, applies an
immutable domain deletion transition and conditionally updates the row. Creation
and deletion share the guard, then acquire character-row locks. No reverse module
dependency, new exported API, cross-module SQL, event or external side effect is
introduced. Existing account-eligibility semantics are preserved; an eligibility
read is not a new concurrency guarantee against future account-status mutations.

Flyway V0006 adds nullable `deleted_at` and an index for active owner reads. Existing
characters begin active without rewriting their state. Deletion sets `deleted_at`
and `updated_at` to the same server-clock instant. The domain rejects repeated or
backdated deletion, and a database check enforces timestamp consistency. Reads
hydrate active domain objects only. The existing name uniqueness constraint is
unchanged and includes deleted rows. No physical deletion, purge, expiry or name
reuse policy is implemented. `deletedByAccountId` is unnecessary in this scope:
only the retained owning `account_id` can perform this operation.

All count/list/detail queries require `deleted_at IS NULL`. Deleted characters
consume no active slot and have no playable/detail representation. Existing DTO
fields are unchanged; deletion metadata remains internal.

The frontend presents a themed native modal dialog, initially focuses Cancel,
traps focus through native dialog behavior, and restores focus when dismissed.
The final action requires the exact case-sensitive name, without trimming.
This is an intentional UX guard, not an additional backend authorization factor.
It warns that the player cannot recover the character and that its name remains
reserved. DELETE automatically carries CSRF and cookies. Success navigates to a
fresh selection list and displays confirmation.

The character ID is the permanent effect identity: a conditional update can only
transition the active row once. Retries do not restore or modify it; repeat DELETE
returns 404 rather than replaying 204. No receipt or idempotency key is needed for
this single irreversible state transition. A transaction failure rolls back both
timestamp changes. A lost response may follow a committed deletion; the frontend
never automatically repeats DELETE and offers a read-only detail check. A 404
after that check means unavailable, not a claim about who deleted the character.

## Consequences and limits

Soft deletion preserves historical data and name reservations indefinitely under
the current policy, but creates no player or administrator recovery mechanism.
Only active characters count toward the cap; retained row count can exceed three.
Later inventory, economy, active tasks or listings must define deletion eligibility
and settlement through their owning APIs before this operation is expanded.
There are no such gameplay obligations in the current implementation.

Real PostgreSQL tests cover owner isolation, CSRF/authentication, hidden reads,
freed slots, reserved names, repeated/concurrent deletion, deletion racing creation,
rollback, empty migration and upgrade from V0005. The browser test deletes a hero,
observes its missing profile, rejects reuse of the name and fills the freed slot.
