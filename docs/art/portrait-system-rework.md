# Character portraits: grounded browser RPG direction

The active set contains six original tier-one portraits: male and female warrior,
archer and mage. Each shows a poor adult beginner in patched cloth with cheap gear.
Faces, silhouettes and equipment occupy a consistent 3:4 frame against a muted
training-yard wall. No copied game assets or external reference artwork were used.

Generated with the built-in imagegen tool, one call per variant, on 2026-11-13.
The original tool outputs are 1086 × 1448 PNGs. The shipped assets are 600 × 800
WebP exports at quality 0.88, resized without cropping or retouching using the
installed Edge canvas encoder through existing Playwright tooling. All six total
489,624 bytes. No runtime image service or additional dependency was added.

## Active files

| Variant | Asset | Source filename |
| --- | --- | --- |
| warrior/male | [tier-1.webp](../../frontend/public/portraits/warrior/male/tier-1.webp) | exec-0ba47020-c370-48cf-bc59-83c7586d2fcd.png |
| warrior/female | [tier-1.webp](../../frontend/public/portraits/warrior/female/tier-1.webp) | exec-0bcb3c5e-75d7-4d21-aeb2-15c011bd8f0e.png |
| archer/male | [tier-1.webp](../../frontend/public/portraits/archer/male/tier-1.webp) | exec-fdc08e71-6f5e-4616-a272-4dee5759f4ec.png |
| archer/female | [tier-1.webp](../../frontend/public/portraits/archer/female/tier-1.webp) | exec-8cab90eb-71f4-482c-9e8b-6870efaa09b1.png |
| mage/male | [tier-1.webp](../../frontend/public/portraits/mage/male/tier-1.webp) | exec-09a643c6-7193-4d03-8ac8-81861ee189bb.png |
| mage/female | [tier-1.webp](../../frontend/public/portraits/mage/female/tier-1.webp) | exec-c929cb1f-87da-46c9-9364-ade5ae81a2ba.png |

Original generation files remain under
`C:/Users/Darek/.codex/generated_images/01a1d1b8-20cf-7ee2-aa3c-412c4fd8f6c7/`.
The application only references workspace WebP files. The earlier class-only PNGs
and standalone beginner-warrior experiment are retained as superseded artwork;
the runtime manifest no longer references them.

## Lookup and future artwork

`getCharacterPortrait(characterClass, gender, level)` selects the class, saved
appearance gender and twenty-level tier. The manifest explicitly registers only
shipped assets. Tier 1 is required for every class/gender pair; later tiers are
optional. If a requested tier is absent, search downward within the same class
and gender. Never change gender or use an unshipped filename as a fallback.

| Levels | Tier | Art direction |
| --- | --- | --- |
| 1–20 | 1 | Patched coarse cloth, rope belts, cheapest sword/bow/wand and book; no armor sets |
| 21–40 | 2 | Repaired, better-fitting clothes; simple belt and serviceable starter weapon |
| 41–60 | 3 | Modest leather protection or sound plain robes; clear class-specific kit |
| 61–80 | 4 | Competent adventurer; practical protective pieces, maintained weapons and tools |
| 81–100 | 5 | Seasoned warrior/scout/caster; durable quality equipment, stronger silhouette |
| 101–120 | 6 | Coordinated practical kit; visibly experienced without decorative overload |
| 121–140 | 7 | Veteran gear, fine materials used sparingly, restrained prestige details |
| 141–160 | 8 | Advanced class equipment; richer but readable silhouette |
| 161–180 | 9 | Elite craftsmanship and distinctive weapons/foci with limited ornament |
| 181–200 | 10 | Strongest legendary class identity, grounded forms, no noisy VFX or huge scenery |

For each new tier, use the corresponding tier-one portrait as an identity reference.
Keep the face, gender, hair identity, camera scale and general background treatment;
upgrade clothing, protection, weapon quality and stance. Both genders receive equal
equipment quality and practical coverage. Check face and class recognition at
small creator-card size as well as the profile. Add `tier-N.webp` under the
appropriate variant directory and register its manifest entry. Add a regression
for fallback around that newly available tier. Do not create empty tier files.

Levels above 200 clamp to tier 10; levels below 1 or non-finite values resolve to
tier 1. Only tier-one artwork ships today. This is visual support, not a leveling
implementation. Equipment/wealth are illustrated tier expectations, not actual
inventory or balance data; neither system exists yet.

## Final prompts

These are the exact prompts supplied to the built-in tool. No source images were supplied.

### warrior/male, tier 1

```text
Use case: stylized-concept.
Asset: an ORIGINAL Steelstory tier-1 character portrait for a classic desktop browser RPG UI. Create one single portrait, vertical 3:4 composition, no frame, no text, no logo.
Style: semi-realistic hand-painted game illustration with deliberately simplified readable shapes, visible restrained brushwork, clear silhouette and face. Grounded classic browser RPG character-slot art, not photographic, not cinematic concept art, not a fantasy book cover or heroic poster. Borrow only the general idea of gear-based visual progression from classic browser RPGs; do not copy any existing game's assets, costumes, poses or compositions.
Composition: one adult character, centered, head through hips, fills most of the image. Face in upper quarter, head fully visible with a small margin, shoulders and key gear fully readable. Relaxed slightly uncertain novice stance, ordinary imperfect face, no glamour. Keep class-defining props close to the torso within the central 75% so they remain visible in UI crops. Flat soft daylight, earth colors, modest contrast, no rim-light spectacle.
Background: low-detail muted plaster training-yard wall with a faint wooden beam; subtle secondary atmospheric hint, no panoramic landscape. Character must dominate.
Critical tier-1 constraint: visibly poor inexperienced beginner, cheap patched coarse cloth, wear and repairs, unadorned functional starter equipment. Almost no armor. NO ornate trim, expensive jewelry, polished plate, fine fabrics, elite armor, royal status, glowing weapons, elaborate VFX, beauty fashion lighting, sexualization, gacha/anime, grimdark horror. Male and female variants must have equal practical coverage and equal poverty.
Subject: Adult male novice warrior, short uneven brown hair, clean-shaven weathered ordinary face, sturdy but untrained build. Loose patched dull oatmeal linen tunic, frayed short sleeves, simple rope belt, worn brown trousers. One cheap short iron sword with plain straight crossguard and wrapped grip, held casually downward across the lower torso; no shield, no other weapons, no metal armor. Alert but unsure expression.
```

### warrior/female, tier 1

```text
Use case: stylized-concept.
Asset: an ORIGINAL Steelstory tier-1 character portrait for a classic desktop browser RPG UI. Create one single portrait, vertical 3:4 composition, no frame, no text, no logo.
Style: semi-realistic hand-painted game illustration with deliberately simplified readable shapes, visible restrained brushwork, clear silhouette and face. Grounded classic browser RPG character-slot art, not photographic, not cinematic concept art, not a fantasy book cover or heroic poster. Borrow only the general idea of gear-based visual progression from classic browser RPGs; do not copy any existing game's assets, costumes, poses or compositions.
Composition: one adult character, centered, head through hips, fills most of the image. Face in upper quarter, head fully visible with a small margin, shoulders and key gear fully readable. Relaxed slightly uncertain novice stance, ordinary imperfect face, no glamour. Keep class-defining props close to the torso within the central 75% so they remain visible in UI crops. Flat soft daylight, earth colors, modest contrast, no rim-light spectacle.
Background: low-detail muted plaster training-yard wall with a faint wooden beam; subtle secondary atmospheric hint, no panoramic landscape. Character must dominate.
Critical tier-1 constraint: visibly poor inexperienced beginner, cheap patched coarse cloth, wear and repairs, unadorned functional starter equipment. Almost no armor. NO ornate trim, expensive jewelry, polished plate, fine fabrics, elite armor, royal status, glowing weapons, elaborate VFX, beauty fashion lighting, sexualization, gacha/anime, grimdark horror. Male and female variants must have equal practical coverage and equal poverty.
Subject: Adult female novice warrior, practical tied-back dark brown hair with loose strands, broad ordinary face, sturdy build. Patched coarse muted rust tunic with full modest coverage, rolled sleeves, plain rope belt, worn brown trousers. One simple short iron sword with plain crossguard and wrapped grip held downward alongside the lower torso, small worn leather wrist wrap; no shield, no other weapons, no metal armor. Determined but inexperienced expression, no makeup.
```

### archer/male, tier 1

```text
Use case: stylized-concept.
Asset: an ORIGINAL Steelstory tier-1 character portrait for a classic desktop browser RPG UI. Create one single portrait, vertical 3:4 composition, no frame, no text, no logo.
Style: semi-realistic hand-painted game illustration with deliberately simplified readable shapes, visible restrained brushwork, clear silhouette and face. Grounded classic browser RPG character-slot art, not photographic, not cinematic concept art, not a fantasy book cover or heroic poster. Borrow only the general idea of gear-based visual progression from classic browser RPGs; do not copy any existing game's assets, costumes, poses or compositions.
Composition: one adult character, centered, head through hips, fills most of the image. Face in upper quarter, head fully visible with a small margin, shoulders and key gear fully readable. Relaxed slightly uncertain novice stance, ordinary imperfect face, no glamour. Keep class-defining props close to the torso within the central 75% so they remain visible in UI crops. Flat soft daylight, earth colors, modest contrast, no rim-light spectacle.
Background: low-detail muted plaster training-yard wall with a faint wooden beam; subtle secondary atmospheric hint, no panoramic landscape. Character must dominate.
Critical tier-1 constraint: visibly poor inexperienced beginner, cheap patched coarse cloth, wear and repairs, unadorned functional starter equipment. Almost no armor. NO ornate trim, expensive jewelry, polished plate, fine fabrics, elite armor, royal status, glowing weapons, elaborate VFX, beauty fashion lighting, sexualization, gacha/anime, grimdark horror. Male and female variants must have equal practical coverage and equal poverty.
Subject: Adult male novice archer, lean build, short untidy dark hair under a rough loose hood, ordinary narrow face. Faded moss-gray coarse hooded shoulder cape over a patched tan cloth shirt, simple second-hand leather strap, rope belt. A plain narrow wooden bow held upright close beside the torso, its simple string visible, small crude quiver with a few uneven feathered arrows behind the shoulder. Poor village scout, cautious observant expression. No ornate buckles or armor.
```

### archer/female, tier 1

```text
Use case: stylized-concept.
Asset: an ORIGINAL Steelstory tier-1 character portrait for a classic desktop browser RPG UI. Create one single portrait, vertical 3:4 composition, no frame, no text, no logo.
Style: semi-realistic hand-painted game illustration with deliberately simplified readable shapes, visible restrained brushwork, clear silhouette and face. Grounded classic browser RPG character-slot art, not photographic, not cinematic concept art, not a fantasy book cover or heroic poster. Borrow only the general idea of gear-based visual progression from classic browser RPGs; do not copy any existing game's assets, costumes, poses or compositions.
Composition: one adult character, centered, head through hips, fills most of the image. Face in upper quarter, head fully visible with a small margin, shoulders and key gear fully readable. Relaxed slightly uncertain novice stance, ordinary imperfect face, no glamour. Keep class-defining props close to the torso within the central 75% so they remain visible in UI crops. Flat soft daylight, earth colors, modest contrast, no rim-light spectacle.
Background: low-detail muted plaster training-yard wall with a faint wooden beam; subtle secondary atmospheric hint, no panoramic landscape. Character must dominate.
Critical tier-1 constraint: visibly poor inexperienced beginner, cheap patched coarse cloth, wear and repairs, unadorned functional starter equipment. Almost no armor. NO ornate trim, expensive jewelry, polished plate, fine fabrics, elite armor, royal status, glowing weapons, elaborate VFX, beauty fashion lighting, sexualization, gacha/anime, grimdark horror. Male and female variants must have equal practical coverage and equal poverty.
Subject: Adult female novice archer, lean build, ordinary freckled face, practical loosely tied auburn hair partly under a rough hood. Faded gray-olive coarse hooded short cape, patched plain tan cloth shirt with full modest coverage, worn simple leather shoulder strap and rope belt. A plain wooden bow held upright close beside the torso with visible simple string, small rough quiver with just a few feathered arrows behind shoulder. Poor novice huntress, watchful uncertain expression, no makeup, no fitted premium ranger outfit.
```

### mage/male, tier 1

```text
Use case: stylized-concept.
Asset: an ORIGINAL Steelstory tier-1 character portrait for a classic desktop browser RPG UI. Create one single portrait, vertical 3:4 composition, no frame, no text, no logo.
Style: semi-realistic hand-painted game illustration with deliberately simplified readable shapes, visible restrained brushwork, clear silhouette and face. Grounded classic browser RPG character-slot art, not photographic, not cinematic concept art, not a fantasy book cover or heroic poster. Borrow only the general idea of gear-based visual progression from classic browser RPGs; do not copy any existing game's assets, costumes, poses or compositions.
Composition: one adult character, centered, head through hips, fills most of the image. Face in upper quarter, head fully visible with a small margin, shoulders and key gear fully readable. Relaxed slightly uncertain novice stance, ordinary imperfect face, no glamour. Keep class-defining props close to the torso within the central 75% so they remain visible in UI crops. Flat soft daylight, earth colors, modest contrast, no rim-light spectacle.
Background: low-detail muted plaster training-yard wall with a faint wooden beam; subtle secondary atmospheric hint, no panoramic landscape. Character must dominate.
Critical tier-1 constraint: visibly poor inexperienced beginner, cheap patched coarse cloth, wear and repairs, unadorned functional starter equipment. Almost no armor. NO ornate trim, expensive jewelry, polished plate, fine fabrics, elite armor, royal status, glowing weapons, elaborate VFX, beauty fashion lighting, sexualization, gacha/anime, grimdark horror. Male and female variants must have equal practical coverage and equal poverty.
Subject: Adult male apprentice mage, young adult with ordinary round face and untidy dark hair, slight build. Plain patched gray-brown coarse wool robe, rough collar, cord sash, visibly frayed cuffs. One short plain slightly crooked wooden wand in a hand held near chest; a small cheap closed starter spellbook with scuffed undecorated brown cover and cloth ties held against lower torso. Poor hedge apprentice, curious hesitant expression. No staff, embroidery, jewelry, magical glow or luxurious regalia.
```

### mage/female, tier 1

```text
Use case: stylized-concept.
Asset: an ORIGINAL Steelstory tier-1 character portrait for a classic desktop browser RPG UI. Create one single portrait, vertical 3:4 composition, no frame, no text, no logo.
Style: semi-realistic hand-painted game illustration with deliberately simplified readable shapes, visible restrained brushwork, clear silhouette and face. Grounded classic browser RPG character-slot art, not photographic, not cinematic concept art, not a fantasy book cover or heroic poster. Borrow only the general idea of gear-based visual progression from classic browser RPGs; do not copy any existing game's assets, costumes, poses or compositions.
Composition: one adult character, centered, head through hips, fills most of the image. Face in upper quarter, head fully visible with a small margin, shoulders and key gear fully readable. Relaxed slightly uncertain novice stance, ordinary imperfect face, no glamour. Keep class-defining props close to the torso within the central 75% so they remain visible in UI crops. Flat soft daylight, earth colors, modest contrast, no rim-light spectacle.
Background: low-detail muted plaster training-yard wall with a faint wooden beam; subtle secondary atmospheric hint, no panoramic landscape. Character must dominate.
Critical tier-1 constraint: visibly poor inexperienced beginner, cheap patched coarse cloth, wear and repairs, unadorned functional starter equipment. Almost no armor. NO ornate trim, expensive jewelry, polished plate, fine fabrics, elite armor, royal status, glowing weapons, elaborate VFX, beauty fashion lighting, sexualization, gacha/anime, grimdark horror. Male and female variants must have equal practical coverage and equal poverty.
Subject: Adult female apprentice mage, ordinary angular face, untidy dark braid, slight build. Plain patched dusty blue-gray coarse robe, rough modest high neckline, simple cord sash, frayed cuffs. One short plain wooden twig wand held near chest; a small cheap closed starter spellbook with scuffed undecorated brown cover and cloth ties held against lower torso. Poor village apprentice, thoughtful hesitant expression. No makeup, staff, embroidery, jewelry, magical glow or luxurious regalia.
```
