package com.steelstory.character.internal.domain;

public record CharacterAttributes(int strength, int dexterity, int intelligence, int constitution, int luck) {
    public CharacterAttributes {
        if (strength < 0 || dexterity < 0 || intelligence < 0 || constitution < 0 || luck < 0) {
            throw new InvalidCharacterInputException("Attribute values cannot be negative.");
        }
    }

    public long total() {
        return (long) strength + dexterity + intelligence + constitution + luck;
    }
}
