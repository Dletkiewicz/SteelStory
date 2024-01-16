package pl.steelstory.model.character.dto;

import pl.steelstory.model.character.CharacterClassType;

import java.util.UUID;

public record CharacterDto(UUID businessId, String name, int level, long experience, CharacterClassType characterClass, int strength, int dexterity, int intelligence, int stamina) {

}
