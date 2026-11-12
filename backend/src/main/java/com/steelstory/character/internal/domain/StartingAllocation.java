package com.steelstory.character.internal.domain;

public record StartingAllocation(CharacterAttributes points) {
    public StartingAllocation {
        if (points == null || points.total() != 10) {
            throw new InvalidCharacterInputException("Distribute exactly 10 starting points.");
        }
    }

    public CharacterAttributes startingAttributes() {
        return new CharacterAttributes(5 + points.strength(),
                5 + points.dexterity(),
                5 + points.intelligence(),
                5 + points.constitution(),
                5 + points.luck());
    }
}
