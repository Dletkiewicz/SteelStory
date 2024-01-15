package pl.steelstory.character.model.dto;

import pl.steelstory.character.model.CharacterClassType;

public record CreateCharacterDto(String name, CharacterClassType characterClass) {
}
