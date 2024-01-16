package pl.steelstory.model.character.dto;

import pl.steelstory.model.character.CharacterClassType;

public record CreateCharacterDto(String name, CharacterClassType characterClass) {

}
