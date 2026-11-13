import assert from 'node:assert/strict';
import { existsSync } from 'node:fs';
import { test } from 'node:test';
import { getCharacterPortrait, portraitTier } from '../../src/characters/portraits.ts';
import { appearanceGenders, characterClasses } from '../../src/characters/creation.ts';

test('each twenty-level interval resolves to its intended portrait tier', () => {
  for (let tier = 1; tier <= 10; tier++) {
    assert.equal(portraitTier((tier - 1) * 20 + 1), tier);
    assert.equal(portraitTier(tier * 20), tier);
  }
  assert.equal(portraitTier(0), 1);
  assert.equal(portraitTier(-10), 1);
  assert.equal(portraitTier(Number.NaN), 1);
  assert.equal(portraitTier(201), 10);
});

test('every class and gender resolves a shipped asset and keeps its identity across missing tiers', () => {
  const sources = new Set<string>();
  for (const characterClass of characterClasses) {
    for (const gender of appearanceGenders) {
      const first = getCharacterPortrait(characterClass, gender, 1);
      sources.add(first);
      assert.equal(first, `/portraits/${characterClass.toLowerCase()}/${gender.toLowerCase()}/tier-1.webp`);
      assert.ok(existsSync(new URL(`../../public${first}`, import.meta.url)));
      for (let level = 1; level <= 220; level++) {
        assert.equal(getCharacterPortrait(characterClass, gender, level), first);
      }
    }
  }
  assert.equal(sources.size, 6);
});
