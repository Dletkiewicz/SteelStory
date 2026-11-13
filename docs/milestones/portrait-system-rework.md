# Character portrait system rework

Implemented and verified locally on 2026-11-13. This extends the earlier deletion
and class-portrait work without committing or pushing it.

## Player-visible changes

Six original beginner portraits replace the active class-only artwork: male and
female warriors, archers and mages. Characters wear patched coarse cloth and
carry plain starter equipment against restrained walls. The portraits use a
consistent 3:4 composition, retaining the gear in creator cards, selection cards
and profiles. The six 600×800 WebP files total 489,624 bytes.

Creation now offers Mężczyzna/Kobieta with immediate portrait previews. Switching
appearance leaves class, name and allocation intact. The choice is saved in the
backend and survives profile reload and login. It is cosmetic; no attributes,
equipment, currency or progression rules changed.

The earlier standalone poor-warrior request produced
`frontend/public/portraits/warrior/tier-1-beginner.png`, an unused experiment.
This rework supplies a complete six-image set instead. Earlier PNGs are retained
for provenance, but no active UI path references them.

## Backend and compatibility

`AppearanceGender` is an immutable character-domain value. V0007 adds a constrained
NOT NULL column and backfills active and deleted characters: ARCHER gets FEMALE,
WARRIOR/MAGE get MALE, matching their previous portraits. Existing data and
timestamps are preserved; applied V0001–V0006 files are unchanged by this step.

Creation accepts optional `appearanceGender`; omitted/null values retain the
legacy defaults, while explicit invalid values return 400. The new UI always
sends a choice. Creation/list/detail responses include the saved field. Existing
paths, operation IDs, ownership/session/CSRF checks, deletion behavior and module
dependencies remain unchanged. OpenAPI and the small TypeScript adapter were
updated deliberately. See [ADR 0006](../adr/0006-character-appearance.md).

This is a coordinated local upgrade. Start the new backend after V0007; an older
binary cannot create characters because it does not insert the required column.
No rolling-production-upgrade compatibility is claimed.

## Artwork, files and future tiers

All images were generated with the built-in imagegen tool, without copied game
assets or reference images. Native PNG outputs were exported to WebP using the
already installed Playwright/Edge canvas encoder, with resizing but no cropping
or retouching. No new dependency or image service was added. All six final prompts,
source filenames, output links and tier-by-tier future directions are in the
[art guide](../art/portrait-system-rework.md).

Active paths are `frontend/public/portraits/{warrior,archer,mage}/{male,female}/tier-1.webp`.
`getCharacterPortrait(characterClass, gender, level)` supports ten twenty-level
tiers. Missing tiers fall back to the nearest earlier registered asset of the
same class and gender. Only tier 1 ships; tiers 2–10 need real artwork and manifest
entries. No placeholder copies or broken future URLs were added. Actual gameplay
leveling, appearance editing and inventory-derived portraits remain outside scope.

Files added or changed in this rework:

- Backend domain/application: `AppearanceGender.java`, `PlayerCharacter.java`,
  `CreateCharacterCommand.java`, `CharacterService.java`.
- Backend adapters/contracts: `JdbcCharacterRepository.java`, `CharacterDtoMapper.java`,
  `CreateCharacterRequestDto.java`, `CharacterResponseDto.java`, `docs/api/openapi.json`.
- Migration: `V0007__character_appearance_gender.sql`.
- Backend tests: `CharacterAppearanceMigrationIT.java`, `CharacterIT.java`,
  `CharacterMigrationIT.java`, `CharacterDeletionMigrationIT.java`,
  `CharacterDomainTest.java`, `FoundationIT.java`. The V0006 deletion-specific test
  is now explicitly targeted at V0006 so its assertions remain about that migration.
- Frontend: `src/api/contracts.ts`, `src/characters/portraits.ts`, `creation.ts`,
  `CharacterCreator.tsx`, `CharacterSelection.tsx`, `CharacterProfile.tsx`,
  `src/ui/CharacterPortrait.tsx`, `src/style.css`, plus the six WebP assets above.
- Frontend tests: unit `portraits.test.ts`, `creation.test.ts`, `client.test.ts`;
  browser `fixtures.ts`, `ui.spec.ts`, `integration.spec.ts`.
- Documentation: README, AGENTS, architecture, domain model, development guidelines,
  ADR 0006, current art guide, superseded-art notice in `class-portraits.md`, this report.

Earlier deletion changes remain in the working tree; they are not a new feature
of this rework. No unrelated dependencies, planned modules or gameplay were added.

## Verification

Final results:

| Command | Result |
| --- | --- |
| Backend `.\mvnw.cmd --batch-mode --no-transfer-progress verify` | PASS: 74 unit/architecture/security tests + 33 PostgreSQL integration tests, zero failures/skips |
| Frontend `npx --yes npm@10.8.2 ci` | PASS, zero reported vulnerabilities |
| `npx --yes npm@10.8.2 run typecheck` (also invoked by build) | PASS in final build |
| `npx --yes npm@10.8.2 run lint` | PASS |
| `npx --yes npm@10.8.2 test` | PASS: 13 tests |
| `npx --yes npm@10.8.2 run test:ui` | PASS: 15 Edge browser tests |
| `npx --yes npm@10.8.2 run test:e2e` | PASS: real registration/login, creation with saved appearance, reload, deletion/name reservation and slot replacement |
| `npx --yes npm@10.8.2 run build` | PASS |
| `git diff --check` and final scoped diff review | PASS |

Browser runs used `$env:PLAYWRIGHT_CHANNEL = 'msedge'`. The initial style check
found line-length violations in new code; they were corrected. An initial
TypeScript check found a test tuple inference issue; it was corrected. A concurrent
frontend installation temporarily left dependencies incomplete, causing build/test
startup failures; a successful clean install restored them before the final passes.
No checks were suppressed. Logs are under ignored `backend/target/portrait-rework-verify.log`;
browser screenshots are under ignored `frontend/test-results/` (each run replaces them).

Tests cover all six API combinations, omitted/null compatibility, invalid values,
owner-only reads, stored values, V0006-to-V0007 migration with active/deleted rows,
unchanged prior checksums/data, database constraints, all tier boundaries and
same-appearance fallback, loading all six local images, preserved allocation, and
saved appearance after real-backend reload. Desktop and 360px mobile screenshots
were inspected. Firefox/Safari and hosted CI were not run. Synthetic E2E account
and character history remain in the local database; it was not reset.

Resolved toolchain unchanged: Temurin 25.0.5+8, Maven 3.9.16 (Wrapper 3.3.4),
Spring Boot 4.1.2, Modulith 2.1.2, springdoc 3.2.0, PostgreSQL 18.6,
Docker 29.0.1, Node 24.22.2, npm 10.8.2, React/DOM 19.3.1,
TypeScript 7.0.2, Vite 8.3.6, Tailwind 4.3.5, Oxlint 1.93.0,
Playwright 1.65.0. No dependency versions changed.

## Run locally in PowerShell

Start Docker Desktop. From the repository root:

```powershell
docker compose up -d --wait
```

In a backend terminal:

```powershell
cd C:\Users\Darek\Desktop\steelstory\backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

In a frontend terminal (stop Vite before reinstalling packages):

```powershell
cd C:\Users\Darek\Desktop\steelstory\frontend
npx --yes npm@10.8.2 ci
npx --yes npm@10.8.2 run dev
```

Open http://127.0.0.1:5173, register if necessary, then log in. Backend startup
applies V0007 automatically; no reset or manual SQL is needed. Use JDK 25 and the
pinned Node version. If those processes already run, open the URL directly rather
than starting duplicates. Stop terminal processes with Ctrl+C; `docker compose stop`
preserves the database. Local frontend and backend were restored for delivery.
