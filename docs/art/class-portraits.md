# Original class portraits

Superseded by the [six-variant portrait rework](portrait-system-rework.md). These earlier PNGs and prompts are retained for provenance and are no longer used by the runtime manifest.

Created 2026-11-13 with the built-in imagegen tool (not the CLI). No reference images, game assets, logos or external downloads were used. Generated files were copied into this repository; originals remain in Codex's generated-images directory. The three shipped square PNG files are 1254 × 1254 pixels, about 9.4 MB combined. No production package or remote asset service is required.

| Class | Repository asset |
| --- | --- |
| WARRIOR | `frontend/public/portraits/warrior/tier-1.png` |
| ARCHER | `frontend/public/portraits/archer/tier-1.png` |
| MAGE | `frontend/public/portraits/mage/tier-1.png` |

`getCharacterPortrait(class, level)` uses a manifest of shipped assets. The requested tier is `ceil(level / 20)`, clamped to 1–10. Missing tiers resolve to the latest earlier asset; today every supported level uses tier one. To add a later tier, add an original asset and register its path in `frontend/src/characters/portraits.ts`. Do not add duplicate files to imply progression artwork exists. These portraits convey class, not actual equipped items or inventory state.

## Exact generation prompts

### warrior

Use case: stylized-concept. Asset: original tier-1 character portrait for Steelstory, a classic medieval fantasy browser RPG. Semi-realistic hand-painted illustration with believable anatomy, expressive face, finely rendered materials and subtle brushwork, like an illustrated fantasy book. Single character, waist-up, centered face fully visible near upper third, square composition with breathing room. Warm daylight, light parchment beige and muted bronze palette, softly painted sunlit medieval landscape background. Beginner adventurer equipment, restrained and practical. Not dark fantasy, no black background, no neon, no text, no lettering, no watermark, no UI frame, no heraldic shield. Original design, not a character or asset from any existing game. A sturdy adult male warrior with chestnut hair and a short beard, calm determined expression, simple mail shirt, worn leather straps and plain steel shoulder armor, one plain sword held beside his shoulder.

### archer

Use case: stylized-concept. Asset: original tier-1 character portrait for Steelstory, a classic medieval fantasy browser RPG. Semi-realistic hand-painted illustration with believable anatomy, expressive face, finely rendered materials and subtle brushwork, like an illustrated fantasy book. Single character, waist-up, centered face fully visible near upper third, square composition with breathing room. Warm daylight, light parchment beige and muted bronze palette, softly painted sunlit medieval landscape background. Beginner adventurer equipment, restrained and practical. Not dark fantasy, no black background, no neon, no text, no lettering, no watermark, no UI frame, no heraldic shield. Original design, not a character or asset from any existing game. An alert adult female archer with auburn hair, green-brown travelling cloak and practical leather armor, wooden longbow visible beside her shoulder and a modest quiver, warm living expression.

### mage

Use case: stylized-concept. Asset: original tier-1 character portrait for Steelstory, a classic medieval fantasy browser RPG. Semi-realistic hand-painted illustration with believable anatomy, expressive face, finely rendered materials and subtle brushwork, like an illustrated fantasy book. Single character, waist-up, centered face fully visible near upper third, square composition with breathing room. Warm daylight, light parchment beige and muted bronze palette, softly painted sunlit medieval landscape background. Beginner adventurer equipment, restrained and practical. Not dark fantasy, no black background, no neon, no text, no lettering, no watermark, no UI frame, no heraldic shield. Original design, not a character or asset from any existing game. A thoughtful adult male mage with dark shoulder-length hair, neatly trimmed beard, ivory and muted blue robes with small bronze clasps, a wooden wand and a worn spellbook, gentle scholarly expression, very subtle warm magical light.
