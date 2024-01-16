package pl.steelstory.model.character.dto;

import lombok.Getter;
import pl.steelstory.model.character.CharacterClassType;

@Getter
public class CreateCharacterDto {

  private String name;
  private CharacterClassType characterClass;
}
