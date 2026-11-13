# ADR 0007: Lifetime XP, provisional levels and local development tools

- Date: 2026-11-13
- Status: Accepted for Milestone 3A; XP curve is provisional, not final game balance

## Domain and representation

Character owns total lifetime `experience`, level and unspent training points.
The existing columns suffice; no migration or data reset is necessary. The domain
validates restored XP/level consistency instead of silently repairing data.

`ExperienceCurve` is the single curve policy: advancing from level L requires
`100 * L * L` XP. The cumulative threshold for level N is
`100 * (N - 1) * N * (2 * N - 1) / 6`, evaluated in long arithmetic for N in 1–200.
Examples: level 1 = 0, level 2 = 100, level 3 = 500, level 4 = 1,400,
level 10 = 28,500, level 200 = 264,670,000. This curve is temporary and subject to
future game balancing. A future curve change must explicitly address existing
XP/level/earned points; replacing the formula alone can invalidate stored state.

`PlayerCharacter.gainExperience(amount, now)` rejects nonpositive amounts,
deleted characters and backdated timestamps. It accepts the smaller of the award
and remaining room below the cap, before adding, so even Long.MAX_VALUE cannot
overflow. A bounded search finds the resulting level; its difference from the
previous level grants exactly three unspent points per completed level-up. At
level 200 no further XP or points are accepted and updatedAt remains unchanged.

The immutable result preserves identity, owner, class, appearance, creation time,
starting attributes and trained bonuses. Level 1 has no level-up points; reaching
200 earns 597, excluding the ten creation points already allocated. Unspent points
cannot exceed level-up earnings and are never negative. This does not cap any
individual attribute or define a point-spending formula. Teachers, currency costs
and spending remain future work. Maximum-level summaries use zero for next-level
requirement/progress/remaining plus an explicit maximumLevel flag; there is no
level 201 target.

## Application and locking

`CharacterService.awardExperience(AccountId, CharacterId, long)` is a trusted
internal application entry point. The caller must obtain amounts from authoritative
server logic, not a production player request. No other module currently needs
this operation, so no speculative module API is exported. A future combat/quest
caller must introduce a narrow named character API with immutable boundary values
and explicit allowlist changes; it must never import `character.internal`.

One transaction acquires the character-owned account guard, checks active account
eligibility through `identity::api`, loads the active owned row with SELECT FOR
UPDATE, executes the aggregate operation and persists only progression fields.
The application result includes previous level, accepted XP, awarded points and
the updated internal aggregate, which is explicitly mapped before HTTP output.

Deletion now also reads its character FOR UPDATE, following the same account-guard
then character-row order. This serializes creation, deletion and awards for one
account. It is intentionally coarser than per-character locking and matches current
conventions. If an award wins, deletion retains its progressed history; if deletion
wins, the award returns not found. No resurrection, lost increments or duplicate
level-up accounting occurs. Transaction failure rolls back all progression fields.
Account eligibility reads retain existing semantics; this adds no atomic exclusion
against hypothetical future account-status changes.

## Event delivery limitations

Row locking provides concurrency safety, **not event idempotency**. Two legitimate
calls are two awards; retrying an already committed call would grant again until
the cap. There is no combat/event source yet, so no reward ledger/event bus is built.
Before asynchronous rewards exist, their authoritative source must provide a stable
business event identity. Enforce permanent uniqueness of its XP effect and record
the deduplication receipt atomically with the character update. Retries must verify
the original payload and return the recorded outcome. Integrate lock ordering and
source settlement in that future slice; an in-memory flag is insufficient.

## Local HTTP boundary

`DevelopmentExperienceController` is registered only when BOTH the Spring `local`
profile and `steelstory.dev.xp.enabled=true` are present. Missing/false property is
disabled, including under local. Without local, setting the property cannot expose
the endpoint. The security filter chain is unchanged.

`POST /api/v1/dev/characters/{id}/grant-xp` accepts `{"amount":500}` and returns
previousLevel, currentLevel, levelsGained, acceptedExperience, trainingPointsAwarded,
totalExperience and unspentTrainingPoints. The development transport accepts exact
integers from 1 through 1,000,000,000; trusted internal awards support positive longs.
It uses the authenticated owner, existing session/active-account checks and CSRF.
Unknown, deleted and unowned targets return 404; invalid input is 400. Valid CSRF
with no session returns 401; missing/invalid CSRF returns 403 before the mutation.
Each request is explicitly a fresh development award, not an idempotent retry.

Normal character responses add `progression` containing currentLevelExperience,
nextLevelExperience, remainingExperience and maximumLevel. Existing fields/operation
IDs remain intact. Standard OpenAPI excludes the disabled dev route; opting in
locally also exposes its generated documentation. Request/response DTOs, mappers,
application/domain independence and Modulith checks remain enforced.

## Frontend and production exclusion

The parchment profile displays lifetime XP separately from current-level progress,
remaining XP and unspent points. A bronze accessible progressbar uses backend
summary values; TypeScript contains no XP curve. At maximum it is full and labeled
Poziom maksymalny. Existing class/gender/tier portraits consume the persisted level,
retain their fallback and grant no gameplay bonuses. Artwork is unchanged.

The testing panel requires Vite DEV, explicit `VITE_DEV_XP_ENABLED=true`, and a
loopback browser hostname. A conditional lazy import removes the dev UI/API module
from production output; the build checks for forbidden panel/endpoint markers,
even with the opt-in environment variable set during the production build.
The ordinary CSRF/cookie client supplies its transport. A synchronous pending guard
prevents double clicks. On success the panel reads actual character state before
updating the profile. Feedback reports actual accepted XP/levels/points only.
No automatic mutation retry occurs; uncertain outcomes require a read-only refresh
with an explicit warning that the next grant is a new reward. A disabled endpoint
or unavailable character disables grants and explains the local configuration.

No animation or flashing effect is introduced. Production has no dev grant controls,
public XP mutation or invented source of XP. Future gameplay can reuse the domain
and application boundary once its authoritative reward/idempotency rules exist.
