package pl.steelstory.model.character;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CharacterClass {

  int strength;
  int dexterity;
  int intelligence;
  CharacterClassType characterClass;

  public CharacterClass() {
    setStrength(5);
    setDexterity(5);
    setIntelligence(5);
  }
}
