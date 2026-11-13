import type { AppearanceGender, Attributes, CharacterClass, CreateCharacterRequest } from '../api/contracts.ts';

export const attributes: { key: keyof Attributes; label: string; short: string }[] = [
  { key: 'strength', label: 'Siła', short: 'STR' },
  { key: 'dexterity', label: 'Zręczność', short: 'DEX' },
  { key: 'intelligence', label: 'Inteligencja', short: 'INT' },
  { key: 'constitution', label: 'Kondycja', short: 'CON' },
  { key: 'luck', label: 'Szczęście', short: 'LUK' },
];
export const classes: Record<CharacterClass, { label: string; description: string; attribute: string }> = {
  WARRIOR: { label: 'Wojownik', description: 'Droga miecza i topora.', attribute: 'Siła' },
  ARCHER: { label: 'Strzelec', description: 'Sztuka łuku i kuszy.', attribute: 'Zręczność' },
  MAGE: { label: 'Mag', description: 'Tajemnice różdżek i ksiąg.', attribute: 'Inteligencja' },
};
export const characterClasses: CharacterClass[] = ['WARRIOR', 'ARCHER', 'MAGE'];
export const appearanceGenders: AppearanceGender[] = ['MALE', 'FEMALE'];
export const genderLabels: Record<AppearanceGender, string> = { MALE: 'Mężczyzna', FEMALE: 'Kobieta' };
export function emptyAllocation(): Attributes {
  return { strength: 0, dexterity: 0, intelligence: 0, constitution: 0, luck: 0 };
}
export function remainingPoints(allocation: Attributes): number {
  return 10 - attributes.reduce((sum, attribute) => sum + allocation[attribute.key], 0);
}
export function adjustAllocation(allocation: Attributes, key: keyof Attributes, delta: 1 | -1): Attributes {
  if (delta === 1 && remainingPoints(allocation) <= 0) return allocation;
  if (delta === -1 && allocation[key] <= 0) return allocation;
  return { ...allocation, [key]: allocation[key] + delta };
}
export function validCharacterName(name: string): boolean {
  return name.length >= 3 && name.length <= 20 && /^[A-Za-z0-9]+(?: [A-Za-z0-9]+)*$/.test(name);
}
export function creationRequest(name: string, characterClass: CharacterClass, appearanceGender: AppearanceGender,
  allocation: Attributes): CreateCharacterRequest {
  if (!validCharacterName(name) || remainingPoints(allocation) !== 0 ||
      attributes.some(({ key }) => !Number.isInteger(allocation[key]) || allocation[key] < 0)) {
    throw new Error('Invalid character creation input');
  }
  return { name, characterClass, appearanceGender, startingAllocation: { ...allocation } };
}
