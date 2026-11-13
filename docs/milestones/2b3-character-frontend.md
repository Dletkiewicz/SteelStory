# Milestone 2B.3 — classic fantasy frontend

Implemented 2026-11-12 under the user's explicit 2B.3 authorization, superseding the
previous frontend stop. Backend creation/list/get remains unchanged. No commit,
push, hosting registration or deployment was performed.

## Delivered experience

Polish registration and login, password visibility and confirmation, restoration
of the current session, three character slots, creation for Warrior/Archer/Mage,
and an owned-character profile. Registration deliberately ends at a success
message and a login link; it does not authenticate the account. The original
backend connectivity check remains in the shared frame.

The creator validates ASCII names and provides bounded plus/minus/reset controls.
Each attribute displays base 5 plus its allocation; exactly ten bonus points must
be spent. All ten may go to one attribute, classes can repeat, and changing class
never changes allocation. Only name, class and bonus allocation are sent. After
creation the list is reloaded and a success message identifies the saved hero.
Returning from a failed/uncertain creation also reloads the authoritative list.

The profile reads its own endpoint, shows level, XP, unspent training points,
starting values, trained bonuses and their permanent sums. Character IDs are URL
navigation context (`#/characters/{id}`), never a new backend session property.
Refresh preserves the profile. Backend ownership checks remain authoritative.

The design uses warm light parchment, brown wood framing, bronze borders, serif
headings, compact attribute rows and original SVG heraldry/landscape. No copied
game assets, remote fonts, hotlinks, emoji artwork or fake future navigation are
used. Mobile navigation becomes a compact horizontal bar and slots stack. Forms
have explicit labels, keyboard focus, native radio groups, live feedback, disabled
submission states and a skip link. No gameplay beyond creation/read is added.

## Implementation and session behavior

- `src/api`: handwritten HTTP contracts, a native-fetch client and safe Polish
  error mapping. No Axios, global state package, router or production dependency
  was added. `src/api.ts` was replaced by this directory; status validation remains.
- `src/auth`: session hook, separate anonymous/loading/authenticated/error states,
  forms and code-point password validation matching the backend. Login accepts
  1–128 characters, registration 15–128; passwords are not trimmed.
- `src/characters`: local list/profile loading, creation interactions and shared
  class/attribute presentation. Client validation is feedback, not authority.
- `src/ui`: shared layout, original class emblems, connection status and errors.
  `App.tsx` only composes session state, hash navigation and feature pages.
- All API paths are relative. Existing `vite.config.ts` already forwards `/api`
  to `http://127.0.0.1:8080`, so it required no change. The local backend profile
  permits HTTP cookies. Use one browser host consistently.
- Startup reads `/auth/me`; 401 is a normal anonymous visit. Protected-request
  401 removes authenticated UI. The account is never inferred from localStorage.
- Before each POST, the client acquires a CSRF token if absent and uses the
  returned `headerName`. Cookies use `credentials: same-origin`; the browser
  handles the HttpOnly SESSION cookie. CSRF exists only in memory.
- Successful login invalidates the old token and fetches a new one. If a network
  failure makes login uncertain, the hook checks `/me` and refreshes CSRF rather
  than replaying credentials. Successful logout handles 204, clears CSRF and
  removes authenticated UI; the next anonymous POST obtains a fresh token.
- 403 clears the cached token and offers explicit read-only session/CSRF refresh.
  It never silently repeats the mutation. Errors keep the form's draft.
- Fetch has a 15-second timeout. No POST is automatically retried. An uncertain
  creation disables resubmission and directs the user to the refreshed list.
  Failed logout keeps the authenticated UI until success/confirmed expiration.
- RFC 9457 codes/statuses map to Polish messages. Raw server details, HTML proxy
  errors, Java traces and security tokens are never rendered or logged.

## Exact local startup (PowerShell)

Use JDK 25, Node **24.22.2** from `.nvmrc`, and npm **10.8.2**. These commands use
`npx --yes npm@10.8.2` to avoid changing a globally installed npm version.

From the repository root, start PostgreSQL:

```powershell
docker compose up -d --wait
```

In a separate terminal from the repository root, start Spring Boot:

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

In another terminal from the repository root, start Vite:

```powershell
cd frontend
npx --yes npm@10.8.2 ci
npx --yes npm@10.8.2 run dev
```

Open **http://127.0.0.1:5173**. The path is browser → Vite → Spring Boot;
Postman and manual cookie/token configuration are unnecessary. Database port
conflicts and matching `DB_URL` overrides are documented in the root README.
No production settings or secrets are introduced. Stop your application terminals
with Ctrl+C; `docker compose stop` preserves local data.

## Manual acceptance

1. Choose **Załóż konto**, enter an unused synthetic local email and a disposable
   15–128 character password, confirm it, and submit. Follow **Przejdź do logowania**.
2. Log in and verify three empty slots. Open **Stwórz postać**.
3. Enter a valid name, choose any class, allocate six points to Siła and four to
   Kondycja. Displayed starting values must be 11, 5, 5, 9, 5. Reset and minus must
   work; unspent points keep submission disabled.
4. Create the hero, confirm the success feedback and refreshed slot, then choose
   **Wybierz postać**. The profile must show those persisted starting values,
   trained bonuses 0, level 1, XP 0 and unspent training points 0.
5. Refresh the profile, return to the slots, and create two more heroes (repeated
   classes are allowed). Verify there is no fourth create action.
6. Log out, log in again, and refresh. Previously created heroes must remain.
7. Check at a narrow mobile width and with keyboard Tab/Enter/arrow keys. A name
   collision must keep the draft and show a Polish error. No tokens are exposed.

## Verification and resolved versions

Executed on Windows with Temurin **25.0.5**, Maven **3.9.16** via Wrapper **3.3.4**,
Node **24.22.2**, npm **10.8.2**, Docker Engine **29.0.1**, PostgreSQL **18.6** and
installed headless Microsoft Edge. The existing backend at port 8080 and Compose
PostgreSQL at 5432 were reused; they were not restarted or stopped.

Runtime/frontend versions retained: Boot **4.1.2**, Modulith **2.1.2**, springdoc
**3.2.0**, React/React DOM **19.3.1**, TypeScript **7.0.2**, Vite **8.3.6**,
plugin-react **6.2.1**, Tailwind/plugin **4.3.5**, Oxlint **1.93.0**. Only development
dependency **@playwright/test 1.65.0** was added, resolving Playwright and
playwright-core 1.65.0. No backend dependency was modified. The official
[Playwright guide](https://playwright.dev/docs/intro) and npm engine metadata were
checked before installation; actual tests verify compatibility on this toolchain.

Commands executed from `backend/`:

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress verify
```

Result: **BUILD SUCCESS**, **73 unit/architecture/security tests + 23 PostgreSQL
integration tests**, zero failures/errors/skips. Includes migration, concurrency,
security and exported OpenAPI comparison. Output is in ignored
`backend/target/2b3-verify.log` and the standard Surefire/Failsafe reports.

Commands executed from `frontend/`:

```powershell
npx --yes npm@10.8.2 install --save-dev --save-exact @playwright/test@1.65.0
npx --yes npm@10.8.2 ci
npx --yes npm@10.8.2 run typecheck
npx --yes npm@10.8.2 run lint
npx --yes npm@10.8.2 test
$env:PLAYWRIGHT_CHANNEL = 'msedge'
npx --yes npm@10.8.2 run test:ui
npx --yes npm@10.8.2 run test:e2e
npx --yes npm@10.8.2 run build
```

Clean install, typecheck, lint and build pass. **9 unit tests**, **8 browser UI
tests** and **1 actual-backend browser acceptance test** pass, with no skips.
The end-to-end test registers through the UI, logs in, creates all three classes,
checks persisted attributes, reloads profiles, logs out/in, restores the list and
checks mobile overflow. Unit tests cover allocation, names, Unicode passwords,
CSRF rotation/invalidations, 204, safe errors and no mutation replay. UI tests cover
slots/cap, payloads, reset/class changes, conflicts, uncertain writes, expiry,
logout failure, CSRF recovery, registration feedback and 360px layout.

Desktop and mobile screenshots were inspected for login, selection, creation and
profile. Tests store screenshots under ignored `frontend/test-results/`; running
a subsequent Playwright command replaces that directory. Test traces are disabled.
The browser runners own and stop their temporary Vite servers. Existing backend
and database processes remain running.

On a machine without Edge, run `npx playwright install chromium`, leave
`PLAYWRIGHT_CHANNEL` unset, then run the tests. CI now runs Node unit and Chromium
UI tests. The actual-backend browser test additionally requires the local backend
and database; it is not yet wired into hosted CI.

## Changed files

Modified existing files:

- `.github/workflows/ci.yaml`, `.gitignore`, `AGENTS.md`, `README.md`
- `docs/architecture.md`, `docs/domain-model.md`, `docs/development-guidelines.md`,
  `docs/adr/0002-tech-stack.md`
- `frontend/index.html`, `frontend/package.json`, `frontend/package-lock.json`,
  `frontend/tsconfig.json`, `frontend/src/App.tsx`, `frontend/src/style.css`

Removed: `frontend/src/api.ts` (replaced by the focused API directory).

Created:

- `docs/milestones/2b3-character-frontend.md`
- `frontend/playwright.config.ts`, `frontend/public/chronicle.svg`
- `frontend/src/api/contracts.ts`, `client.ts`, `errors.ts`
- `frontend/src/auth/AuthPage.tsx`, `useSession.ts`, `validation.ts`
- `frontend/src/characters/CharacterCreator.tsx`, `CharacterProfile.tsx`,
  `CharacterSelection.tsx`, `CharactersPage.tsx`, `creation.ts`, `useCharacters.ts`
- `frontend/src/navigation.ts`
- `frontend/src/ui/ClassEmblem.tsx`, `ConnectionStatus.tsx`, `Feedback.tsx`, `GameLayout.tsx`
- `frontend/tests/unit/client.test.ts`, `creation.test.ts`
- `frontend/tests/browser/fixtures.ts`, `ui.spec.ts`, `integration.spec.ts`

## Limits

No backend sources, migrations or OpenAPI contracts changed. No progression,
training, currency, inventory, equipment, combat, PvP, quests, deletion, respec,
email delivery, administrative recovery or deployment is implemented. Existing
backend public-launch blockers still apply as documented in the README.

Browser verification covers Edge/Chromium on Windows and mobile viewport
emulation; real mobile devices, Firefox, Safari/WebKit, screen-reader review and
hosted Linux CI were not executed. There is no exhaustive accessibility audit.
Tests create synthetic local accounts/characters and intentionally retain them;
no deletion endpoint or direct database cleanup was added. The current numeric
XP contract is preserved (creation yields zero); future progression must review
JavaScript integer precision if values can exceed Number.MAX_SAFE_INTEGER.
