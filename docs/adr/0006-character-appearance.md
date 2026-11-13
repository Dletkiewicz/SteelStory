# ADR 0006: Saved cosmetic appearance and class/gender portrait tiers

- Date: 2026-11-13
- Status: Accepted within the explicitly requested portrait-system rework

## Context

The class-only portraits provided one appearance per class and depicted equipment
too advanced for a beginner. The user requested grounded tier-one artwork for
both male and female warriors, archers and mages, with selection during character
creation and future twenty-level visual tiers. There was no saved appearance field.

## Decision

Store one immutable `AppearanceGender` value (MALE/FEMALE) on `PlayerCharacter`.
It describes the character portrait, not the account holder. Keep it independent
of class, attributes, ownership and eligibility. There is no appearance mutation
endpoint, portrait ID, customization subsystem or new module. Character retains
its only cross-module dependency on `identity::api`.

Flyway V0007 adds `appearance_gender`, backfills existing active and deleted rows
according to their previous portrait (ARCHER female; WARRIOR/MAGE male), then adds
NOT NULL and an explicit allowed-values constraint. All other data, timestamps,
names and migration checksums are preserved. Database inserts must explicitly
provide the value. This is a coordinated local backend/frontend upgrade; the
previous backend binary cannot insert after V0007 because it omits this column.
No production rolling-release compatibility is claimed.

POST creation accepts optional `appearanceGender`. Omitted or JSON null retains
the legacy class-specific appearance; explicit values must be exactly MALE or
FEMALE. This compatibility choice avoids breaking earlier clients. The new UI
always sends its selected value. The domain owns parsing/default policy; the web
mapper creates a typed application command. Creation, list and detail responses
always include `appearanceGender`. Existing fields, paths, errors and operation
IDs remain unchanged. OpenAPI and the handwritten TypeScript contract are updated
deliberately. Invalid values return 400 without creating a character.

The repository persists and hydrates the value through existing character-owned
SQL. Creation's transaction, account guard, active eligibility and cap are unchanged.
Deletion preserves appearance alongside retained history. No new lock or event is
needed for an immutable cosmetic field.

The frontend selects original local artwork with
`getCharacterPortrait(characterClass, gender, level)`. Each class/gender pair has
a mandatory tier-one asset and optional tiers 2–10. Tiers span levels 1–20,
21–40, through 181–200; missing tiers fall back to the nearest earlier registered
asset of the same class/gender. Future tiers are not represented by dummy files
or broken requests. Level is read from the server, not calculated from client XP.

Six 600×800 WebP assets ship now. They depict patched coarse clothing, cheap
starter equipment, ordinary faces and subdued walls instead of scenic vistas.
Creation previews, occupied selection cards and profiles use portraits; the
existing shield remains a navigation emblem and empty-slot decoration. Frames
retain the 3:4 composition to keep class equipment visible. Exact prompts,
provenance, file paths and future art direction are in the
[art guide](../art/portrait-system-rework.md).

## Consequences and verification

Saved appearance survives reload/login and can be reused when later artwork is
added. Existing characters receive a documented default because there was no
previous preference to migrate. No editing feature is implicitly authorized.
Visual equipment quality illustrates a tier; it does not inspect inventory or
wallets. Gameplay progression, equipment and economy remain unimplemented.

Real PostgreSQL tests verify every class/gender combination, invalid input,
legacy omitted/null requests, owner isolation, constraints and V0006 upgrades
with active/deleted rows and unchanged history. Frontend tests cover all six local
assets, tier boundaries and fallbacks, preview switching without resetting class
or allocation, and saved appearance after real-backend creation/reload.
