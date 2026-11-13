package com.steelstory.character.internal.application;

public class CharacterNameTakenException extends RuntimeException {
    public CharacterNameTakenException() {
        super("Character name is already taken.");
    }
}
