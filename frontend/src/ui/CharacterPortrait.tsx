import type { AppearanceGender, CharacterClass } from '../api/contracts';
import { classes, genderLabels } from '../characters/creation';
import { getCharacterPortrait } from '../characters/portraits';

export function CharacterPortrait({ characterClass, gender, level }: {
  characterClass: CharacterClass; gender: AppearanceGender; level: number;
}) {
  return <img className="character-portrait" src={getCharacterPortrait(characterClass, gender, level)}
    alt={`Portret klasy: ${classes[characterClass].label}, ${genderLabels[gender].toLowerCase()}`}
    width={600} height={800} decoding="async" />;
}
