package com.steelstory.character.internal.domain;

public enum CharacterClass {
    WARRIOR, ARCHER, MAGE;

    public static CharacterClass parse(String value) {
        if (value != null) {
            for (CharacterClass characterClass : values()) {
                if (characterClass.name().equals(value)) {
                    return characterClass;
                }
            }
        }

        throw new InvalidCharacterInputException("Character class must be WARRIOR, ARCHER or MAGE.");
    }
}
