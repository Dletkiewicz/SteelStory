import type { AppearanceGender, CharacterClass } from '../api/contracts.ts';

// Register only assets that actually ship. Missing future tiers resolve to the latest earlier portrait.
type PortraitTiers = { 1: string } & Partial<Record<2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10, string>>;
const portraits: Record<CharacterClass, Record<AppearanceGender, PortraitTiers>> = {
  WARRIOR: {
    MALE: { 1: '/portraits/warrior/male/tier-1.webp' },
    FEMALE: { 1: '/portraits/warrior/female/tier-1.webp' },
  },
  ARCHER: {
    MALE: { 1: '/portraits/archer/male/tier-1.webp' },
    FEMALE: { 1: '/portraits/archer/female/tier-1.webp' },
  },
  MAGE: {
    MALE: { 1: '/portraits/mage/male/tier-1.webp' },
    FEMALE: { 1: '/portraits/mage/female/tier-1.webp' },
  },
};

export function portraitTier(level: number): number {
  if (!Number.isFinite(level)) return 1;
  return Math.min(10, Math.max(1, Math.ceil(level / 20)));
}

export function getCharacterPortrait(characterClass: CharacterClass, gender: AppearanceGender, level: number): string {
  const available: Partial<Record<number, string>> = portraits[characterClass][gender];
  for (let tier = portraitTier(level); tier >= 1; tier--) {
    const source = available[tier];
    if (source) return source;
  }
  return portraits[characterClass][gender][1];
}
