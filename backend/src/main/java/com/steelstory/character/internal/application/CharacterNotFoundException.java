package com.steelstory.character.internal.application;

public class CharacterNotFoundException extends RuntimeException {
    public CharacterNotFoundException() {
        super("Character not found.");
    }
}
