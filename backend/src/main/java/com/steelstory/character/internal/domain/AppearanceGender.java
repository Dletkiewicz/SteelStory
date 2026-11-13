package com.steelstory.character.internal.domain;

public enum AppearanceGender {
    MALE, FEMALE;

    public static AppearanceGender parse(String value) {
        for (AppearanceGender gender : values()) {
            if (gender.name().equals(value)) {
                return gender;
            }
        }

        throw new InvalidCharacterInputException("Appearance gender must be MALE or FEMALE.");
    }

    public static AppearanceGender forCreation(String value, CharacterClass characterClass) {
        // Older clients omit this field. Preserve the original class portrait's appearance.
        if (value == null) {
            return characterClass == CharacterClass.ARCHER ? FEMALE : MALE;
        }

        return parse(value);
    }
}
