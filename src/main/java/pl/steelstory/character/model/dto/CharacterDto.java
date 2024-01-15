package pl.steelstory.character.model.dto;

import pl.steelstory.character.model.CharacterClassType;

import java.util.UUID;

public record CharacterDto(UUID businessId, String name, int level, long experience, CharacterClassType characterClass, int strength, int intelligence, int stamina) {

}
