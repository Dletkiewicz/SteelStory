# Milestone 3A: experience, leveling and character progression

Implemented and verified locally on 2026-11-13. No commits, pushes, deployment or
database reset. Existing uncommitted deletion/appearance work was extended in place.
Read [ADR 0007](../adr/0007-character-progression.md) for the detailed decisions.

## Domain, curve and level-up results

`experience` is total lifetime XP. `ExperienceCurve` owns the **provisional
development curve**, not approved final balance: `requiredXp(L) = 100 * L * L`
is the incremental cost from L to L+1. Total thresholds sum those costs:

| Level | Total XP threshold | XP to next level |
| --- | ---: | ---: |
| 1 | 0 | 100 |
| 2 | 100 | 400 |
| 3 | 500 | 900 |
| 4 | 1,400 | 1,600 |
| 20 | 247,000 | 40,000 |
| 21 | 287,000 | 44,100 |
| 40 | 2,054,000 | 160,000 |
| 41 | 2,214,000 | 168,100 |
| 200 | 264,670,000 | None |

The immutable `gainExperience(amount, now)` operation accepts only positive XP
for an active character. It saturates the award before addition, avoiding overflow
even for Long.MAX_VALUE. A bounded search of cumulative thresholds finds the new
level. Three times the level difference is added to unspent points. Thus 500 XP
from level 1 gives level 3 and six points; reaching 200 earns 597 points. Starting
allocation, trained bonuses, class and appearance remain unchanged. Training
points cannot be spent in this milestone; no individual attribute cap is added.

XP caps at 264,670,000. Further valid awards accept zero XP, grant zero points and
do not change updatedAt. The level cannot become 201. Restoration checks XP/level
consistency and nonnegative points bounded by earned level-up points. Deleted
characters and backdated domain updates are rejected. The existing seven
migrations and current columns suffice; **no new migration was added**.

## Application, persistence and concurrency

`CharacterService.awardExperience(AccountId, CharacterId, long)` is the internal
trusted server entry point. It returns previous level, accepted XP, awarded points,
levels gained and the resulting character through a small application result.
There is no production player XP endpoint or arbitrary level setter.

One transaction locks the character-owned account guard, checks eligibility via
Identity's named API, and locks the active owned character with SELECT FOR UPDATE.
It executes domain behavior and saves progression fields. Deletion now uses the
same row lock, following account guard → character row. Independent awards cannot
lose updates or duplicate points for the same level threshold. Deletion either
retains an already completed reward or wins first and makes the award unavailable.
Failures roll back all XP, level, points and timestamp changes.

The graph remains `character -> identity::api`. No speculative public module API
is exported because no other module currently awards XP. A future game system must
add a narrow named API rather than importing `character.internal`.

**Concurrency is not event idempotency.** Every current call is a new reward.
Future combat/expedition delivery needs permanent business-event uniqueness and a
receipt recorded atomically with XP, payload validation on retries and documented
source settlement/lock order. No fabricated event bus or complex ledger was added.

## HTTP security and response changes

The production character DTO adds a read-only `progression` object:

```json
{
  "currentLevelExperience": 40,
  "nextLevelExperience": 100,
  "remainingExperience": 60,
  "maximumLevel": false
}
```

Existing fields and operation IDs remain intact. At maximum the numeric summary
fields are zero and maximumLevel is true. The UI renders a full indicator and no
next-level target. The reviewed normal OpenAPI snapshot includes this addition and
the approved level/XP/points bounds. It excludes the disabled development endpoint.

The separate `DevelopmentExperienceController` exists only with both `local` and
`steelstory.dev.xp.enabled=true`. Missing/false property is disabled; property true
without local cannot register it. Security configuration is unchanged. The endpoint
uses authenticated ownership, active account checks and existing session/CSRF:

```http
POST /api/v1/dev/characters/{id}/grant-xp
Content-Type: application/json
X-CSRF-TOKEN: <token from the existing session flow>

{"amount":500}
```

Amount must be an exact integer from 1 through 1,000,000,000. Success is 200 with
previousLevel, currentLevel, levelsGained, acceptedExperience, trainingPointsAwarded,
totalExperience and unspentTrainingPoints. Invalid input is 400; missing session
with valid CSRF is 401; missing/invalid CSRF is 403; missing/deleted/unowned target
is 404. The enabled local OpenAPI endpoint also documents this route. DTO mappings
are explicit; controllers do not serialize aggregates or update SQL directly.

## Frontend

The parchment/bronze profile now distinguishes total XP from progress within its
current level, shows remaining XP and unspent points, and handles maximum level.
The progressbar has an accessible label/value; no motion, flashing or independent
TypeScript XP curve is introduced. The existing portrait resolver receives the
persisted level and retains all class/gender fallback behavior. Artwork is untouched.
Tier boundaries remain 1–20, 21–40, through 181–200; no tier 11 is requested and
portraits grant no gameplay bonuses.

The separate developer UI/API module requires Vite development, explicit
`VITE_DEV_XP_ENABLED=true` and a loopback browser hostname. Production tree-shaking
removes its dynamic import, UI and route strings; the build performs an exclusion
check. It provides 100/500/custom XP and read-only refresh, uses the ordinary
session/CSRF transport, blocks repeat clicks synchronously and reads saved character
data after success. Level-up feedback reports the actual levels/points awarded.
A non-leveling award does not celebrate. Unknown outcomes require reconciliation,
without automatically repeating POST. Disabled backend tools explain configuration
and disable grants. Point spending, teachers, combat, economy and inventory remain
outside scope.

## Enable, use and disable locally

Use JDK 25, Node 24.22.2 and npm 10.8.2. Start Docker Desktop, then from repo root:

```powershell
docker compose up -d --wait
```

Stop an existing backend before starting its replacement. In a backend terminal:

```powershell
cd C:\Users\Darek\Desktop\steelstory\backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local" "-Dspring-boot.run.arguments=--steelstory.dev.xp.enabled=true"
```

Or use the verified packaged application:

```powershell
java -jar target/steelstory-0.1.0-SNAPSHOT.jar --spring.profiles.active=local --steelstory.dev.xp.enabled=true
```

In a frontend terminal (stop Vite before package installation):

```powershell
cd C:\Users\Darek\Desktop\steelstory\frontend
npx --yes npm@10.8.2 ci
$env:VITE_DEV_XP_ENABLED = 'true'
npx --yes npm@10.8.2 run dev
```

Open http://127.0.0.1:5173, register/login and choose a character. In its developer
panel, grant 500 XP to a fresh character: level becomes 3 and points become 6.
Refresh the page to verify persistence. A custom 1,000,000,000 grant reaches the
cap; another grant accepts zero. These are real local database changes, not a mock.

To disable, stop backend and Vite. Restart the backend with only
`"-Dspring-boot.run.profiles=local"` (omit the XP argument). Remove any XP property
environment override if you set one. In the frontend terminal:

```powershell
Remove-Item Env:VITE_DEV_XP_ENABLED -ErrorAction SilentlyContinue
npx --yes npm@10.8.2 run dev
```

No opt-in is written to committed defaults. The review preview started for this
delivery explicitly enables both flags in its process environment/arguments.

## Verification and exact results

| Check | Result |
| --- | --- |
| Backend Maven validate/Checkstyle | PASS, zero violations |
| Backend Maven verify | PASS: **95** unit/domain/architecture/configuration tests and **38** PostgreSQL integration tests; zero failures/errors/skips |
| Frontend npm ci | PASS, zero reported vulnerabilities |
| Frontend typecheck | PASS |
| Frontend lint (existing Oxlint tooling) | PASS |
| Frontend unit tests | **13 passed** |
| UI and development-panel browser tests | **22 passed** |
| Real-backend browser flows | **2 passed** |
| Production build with frontend opt-in set true | PASS; development UI/API excluded |
| Final diff review and diff --check | PASS |

Backend coverage includes every curve boundary, exact/partial/multiple levels,
Long.MAX_VALUE saturation, invalid/deleted/backdated operations, restore invariants,
preserved class/attributes, persistence/reload, max behavior, independent concurrent
awards, award/deletion races, rollback, owner/session/CSRF/disabled-account security,
the full profile/property opt-in matrix, migrations and existing Identity flows.

Browser coverage includes 40/100 XP with 60 remaining, exact thresholds, max status,
pending double-click prevention, invalid amounts, 403 recovery without replay,
unknown outcome reconciliation, disabled tools, false celebration prevention,
all six portraits, responsive overflow checks, creation/deletion/appearance and
authentication regressions. Real XP testing crosses levels 20/21/40/41, reaches 200,
reloads, verifies 597 points and unchanged attributes. Desktop progression artwork
and layout were inspected. The first new E2E run exposed test timing while switching
authentication screens; it was fixed by awaiting the target heading before filling
shared field labels. The final run passed both flows. No application/security check
was weakened to resolve the test.

Commands executed (from the indicated folders):

```powershell
# backend, Docker running; stop a running packaged JAR before verify on Windows
.\mvnw.cmd --batch-mode --no-transfer-progress validate
.\mvnw.cmd --batch-mode --no-transfer-progress verify

# frontend, stop running Vite first; tests own ports 5173 and 5174
npx --yes npm@10.8.2 ci
npx --yes npm@10.8.2 run typecheck
npx --yes npm@10.8.2 run lint
npx --yes npm@10.8.2 test
$env:PLAYWRIGHT_CHANNEL = 'msedge'
npx --yes npm@10.8.2 run test:ui
# Keep the local backend running with its XP opt-in for the next command:
npx --yes npm@10.8.2 run test:e2e
$env:VITE_DEV_XP_ENABLED = 'true'
npx --yes npm@10.8.2 run build
```

Backend log: ignored `backend/target/progression-verify.log`. Browser artifacts:
ignored `frontend/test-results/`, replaced by each run. Production output is ignored
`frontend/dist/`. Synthetic E2E accounts and characters, including a level-200
character and deleted history, remain in the local database. No database was reset.
Windows Edge was exercised; Firefox/Safari and hosted CI were not run.

Resolved versions remain unchanged: Temurin 25.0.5+8, Maven 3.9.16/Wrapper 3.3.4,
Spring Boot 4.1.2, Modulith 2.1.2, springdoc 3.2.0, PostgreSQL 18.6,
Docker 29.0.1, Node 24.22.2, npm 10.8.2, React/DOM 19.3.1,
TypeScript 7.0.2, Vite 8.3.6, Tailwind 4.3.5, Oxlint 1.93.0, Playwright 1.65.0.
No dependency was added or upgraded.

## Files created and modified in this milestone

New backend files under `backend/src/main/java/com/steelstory/character/internal/`:

- `domain/ExperienceCurve.java`
- `application/ExperienceAwardResult.java`
- `web/DevelopmentExperienceController.java`
- `web/DevelopmentExperienceDtoMapper.java`
- `web/dto/CharacterProgressionResponseDto.java`
- `web/dto/GrantExperienceRequestDto.java`
- `web/dto/GrantExperienceResponseDto.java`

Modified backend files in that same tree:

- `domain/PlayerCharacter.java`
- `application/CharacterService.java`, `CharacterRepository.java`
- `infrastructure/JdbcCharacterRepository.java`
- `web/CharacterDtoMapper.java`, `CharacterExceptionHandler.java`
- `web/dto/CharacterResponseDto.java`

Backend tests: new `character/internal/domain/CharacterProgressionTest.java` and
`character/DevelopmentExperienceAvailabilityTest.java`; extended `character/CharacterIT.java`.
Existing other tests and all migrations are preserved by this milestone.

New frontend files:

- `src/characters/CharacterProgression.tsx`
- `src/dev/DevelopmentExperiencePanel.tsx`, `src/dev/experienceApi.ts`
- `scripts/check-production-build.mjs`
- `tests/browser/progression.spec.ts`

Modified frontend files: `src/api/contracts.ts`, `src/api/client.ts`,
`src/characters/CharacterProfile.tsx`, `src/style.css`, `package.json`,
`playwright.config.ts`, and browser `fixtures.ts`, `ui.spec.ts`, `integration.spec.ts`.
Portrait assets/resolver and dependency lockfile are unchanged by this milestone.

Documentation: README, AGENTS, architecture, domain model, development guidelines,
`docs/api/openapi.json`, new ADR 0007 and this report. Earlier uncommitted deletion
and appearance files remain present; they were not reverted or counted as new 3A work.

## Known limits and next boundaries

Balance is provisional. Rebalancing must deliberately reconcile persisted levels
and earned/spent points; no rules-version or database-driven balance system exists.
Database columns retain earlier broad checks; all application reads/writes validate
the stricter domain invariants. Manual SQL is not an authorized progression path.
The account guard serializes siblings as well as one character; optimize only with
evidence and a reviewed lock order. Asynchronous event idempotency is still required
before real reward sources are introduced. No production gameplay source of XP,
teacher/NPC, point spending, currency charge, combat or reward event bus was added.
