# Character deletion and class portraits

Implemented 2026-11-13 under the explicit next-step request. This extends the
previous 2B.3 stop only with soft deletion and class portraits. No commit, push,
deployment, email delivery, progression or other gameplay was added.

## Backend design

`DELETE /api/v1/characters/{id}` derives the actor from the session, requires CSRF,
and returns 204 with no body. Unknown/unowned/already deleted targets return 404;
unauthenticated/inactive accounts return 401, invalid/missing CSRF 403 and malformed
IDs 400. As with existing mutations, CSRF rejection can precede authentication.
Existing GET/POST DTO shapes remain unchanged.

`CharacterService.deleteOwn` owns one transaction: acquire the existing account
guard, check eligibility via `identity::api`, load the owned active character,
apply immutable domain deletion and conditionally update the row. Sharing the
guard with creation serializes active-slot changes across instances. The domain
rejects repeated/backdated deletion; rollback restores playability.

V0006 adds nullable `deleted_at`, a timestamp constraint and an active-owner index.
Applied migrations V0001–V0005 remain unchanged. Deletion retains all character
data, updates `deleted_at` and `updated_at` together, and preserves the existing
global name constraint. All list/detail/count queries filter `deleted_at IS NULL`.
Only active rows count toward three slots, but retained rows may exceed three.
There are no new module dependencies, cross-module SQL, events or external effects.
Policy and retry semantics are recorded in [ADR 0005](../adr/0005-character-soft-deletion.md).

## Frontend and portraits

**Usuń postać** on the profile opens a themed native modal. It warns that the
player cannot undo deletion and that the name stays reserved. The final action
requires the exact name, including case and spaces. Cancel is initially focused;
Escape/cancel restore focus, native modality keeps background controls inert,
and pending requests disable repeated submission/dismissal.

The client attaches CSRF to DELETE as well as POST. Success returns to a freshly
loaded selection screen with confirmation. 401 removes authenticated UI; 403
retains the draft and offers explicit session/CSRF refresh; 404 disables deletion
and offers return to selection. Uncertain network/server results disable another
attempt until a read-only detail check. No DELETE is automatically replayed.

Three original semi-realistic portraits were generated with the built-in imagegen
tool and saved as `frontend/public/portraits/{warrior,archer,mage}/tier-1.png`.
They appear on occupied selection cards, profiles and creator class choices.
Empty-slot and sidebar heraldry remain decorative UI elements. Exact prompts and
asset provenance are in [class portraits](../art/class-portraits.md).

`portraitTier` maps each twenty-level interval to tiers 1–10.
`getCharacterPortrait(class, level)` resolves the latest available tier at or below
the requested one. Only tier one ships; there are no fake tier files or broken
future-tier requests. Add a new local image and register its tier in `portraits.ts`
to extend artwork. This introduces no leveling or equipment functionality.

## Verification

Toolchain: Temurin **25.0.5**, Maven **3.9.16** / Wrapper **3.3.4**, Node **24.22.2**,
npm **10.8.2**, Docker **29.0.1**, PostgreSQL **18.6**, installed headless Edge.
Dependencies remain Boot **4.1.2**, Modulith **2.1.2**, springdoc **3.2.0**,
React/React DOM **19.3.1**, TypeScript **7.0.2**, Vite **8.3.6**, Tailwind/plugin
**4.3.5**, Oxlint **1.93.0**, Playwright **1.65.0**. No dependencies were added.

Executed from `backend/`:

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress verify
```

Final result: **74 unit/architecture/security tests + 29 PostgreSQL integration
tests pass**, zero failures/errors/skips. Includes owner isolation, active reads,
freed slots, reserved names, repeated/concurrent deletion, delete/create cap races,
rollback, authentication/CSRF, empty migration and populated V0005 upgrade with
retained data/checksums. The reviewed OpenAPI snapshot matches the runtime export.
Reports and `deletion-verify.log` are in ignored `backend/target/`.

Executed from `frontend/`:

```powershell
npx --yes npm@10.8.2 ci
npx --yes npm@10.8.2 run typecheck
npx --yes npm@10.8.2 run lint
npx --yes npm@10.8.2 test
$env:PLAYWRIGHT_CHANNEL = 'msedge'
npx --yes npm@10.8.2 run test:ui
npx --yes npm@10.8.2 run test:e2e
npx --yes npm@10.8.2 run build
```

Final result: install/typecheck/lint/build pass; **13 unit tests, 14 browser UI tests
and one real-backend browser flow pass**. New cases cover DELETE headers/cookies,
204/errors/no replay, tier boundaries/fallbacks, exact-name confirmation, focus,
401/403/404, uncertain results, local image loading, portrait layout and mobile
overflow. The real browser creates three heroes, deletes one, checks the missing
profile and reserved name, then fills the freed slot with a new hero. Desktop and
mobile screenshots were inspected; artifacts are in ignored `frontend/test-results/`.

Intermediate checks found one Checkstyle line-length issue, the expected contract
snapshot change, cancelled-dialog focus restoration and portrait grid overflow;
all were corrected and checked. An initial Windows `npm ci` failed because the
running Vite locked a native module; stopping Vite allowed a clean install. No
checks were disabled or skipped. Hosted CI, Firefox/WebKit and real mobile devices
were not executed.

## Local run and manual check

From the root: `docker compose up -d --wait`. In a separate backend terminal:

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

In a frontend terminal:

```powershell
cd frontend
npx --yes npm@10.8.2 ci
npx --yes npm@10.8.2 run dev
```

Open **http://127.0.0.1:5173**. Stop Vite before `npm ci` on Windows. Restart the
backend to apply V0006; no data reset is needed. Stop a running packaged JAR before
rebuilding it on Windows. The existing proxy/session settings are unchanged.
Verification restarted the local backend and applied V0006 to the existing local
database without physically deleting character data.

At delivery, the replacement backend and Vite processes are running on ports 8080
and 5173, restoring the prior local development state. Frontend HTTP 200, backend
health UP and the live `deleteOwnCharacter` operation were checked. Local logs are
in `backend/target/deletion-local.*.log` and `.tools/deletion-vite.*.log` (ignored).

Use a disposable character: cancel deletion once, reopen it, try incorrect case
and extra spaces, then type the exact name and confirm. Check the freed slot and
404 at the old profile URL. The old name must fail creation; a new name must work.
Check all three portraits on desktop and mobile. Synthetic test records, including
soft-deleted rows, intentionally remain in the local database.

## Limitations

Only tier-one artwork exists. Original PNGs total about 9.4 MB; no WebP variants
are supplied. No email confirmation, recovery, purge, name release, account
deletion or later gameplay is implemented. Depicted weapons are artwork only.
Future gameplay obligations must define deletion eligibility/settlement before
those modules are added. Existing public-launch limitations remain in README.

## Changed files

Backend modified (under `backend/src/main/java/com/steelstory/`):

- `character/internal/application/CharacterRepository.java`, `CharacterService.java`,
  `CharacterLimitReachedException.java`
- `character/internal/domain/PlayerCharacter.java`
- `character/internal/infrastructure/JdbcCharacterRepository.java`
- `character/internal/web/CharacterController.java`

Backend tests modified (under `backend/src/test/java/com/steelstory/`):

- `FoundationIT.java`, `character/CharacterIT.java`, `character/CharacterMigrationIT.java`
- `character/internal/domain/CharacterDomainTest.java`

Backend created:

- `backend/src/main/resources/db/migration/V0006__character_soft_deletion.sql`
- `backend/src/test/java/com/steelstory/character/CharacterDeletionMigrationIT.java`

Frontend modified (under `frontend/`):

- `src/App.tsx`, `src/api/client.ts`, `src/style.css`
- `src/characters/CharacterCreator.tsx`, `CharacterProfile.tsx`,
  `CharacterSelection.tsx`, `CharactersPage.tsx`
- `tests/unit/client.test.ts`, `tests/browser/ui.spec.ts`, `tests/browser/integration.spec.ts`

Frontend created (under `frontend/`):

- `src/characters/CharacterRoutes.tsx`, `DeleteCharacterDialog.tsx`, `portraits.ts`
- `src/ui/CharacterPortrait.tsx`, `tests/unit/portraits.test.ts`
- `public/portraits/warrior/tier-1.png`, `public/portraits/archer/tier-1.png`,
  `public/portraits/mage/tier-1.png`

Documentation modified: `AGENTS.md`, `README.md`, `docs/architecture.md`,
`docs/domain-model.md`, `docs/development-guidelines.md`, `docs/api/openapi.json`.
Documentation created: this report, `docs/adr/0005-character-soft-deletion.md`,
`docs/art/class-portraits.md`.
