package com.steelstory.character.internal.application;

import com.steelstory.character.internal.domain.CharacterClass;
import com.steelstory.character.internal.domain.CharacterName;
import com.steelstory.character.internal.domain.StartingAllocation;

public record CreateCharacterCommand(CharacterName name, CharacterClass characterClass, StartingAllocation allocation) {
}
