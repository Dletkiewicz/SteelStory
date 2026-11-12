package com.steelstory.character.internal.domain;

import java.util.Locale;
import java.util.regex.Pattern;

public record CharacterName(String value) {
    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9]+(?: [A-Za-z0-9]+)*");

    public CharacterName {
        if (value == null || value.length() < 3 || value.length() > 20 || !VALID_NAME.matcher(value).matches()) {
            throw new InvalidCharacterInputException(
                    "Name must be 3-20 ASCII letters/digits with single internal spaces.");
        }
    }

    public String canonical() {
        return value.toLowerCase(Locale.ROOT);
    }
}
