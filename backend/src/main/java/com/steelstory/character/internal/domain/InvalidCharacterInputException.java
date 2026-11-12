package com.steelstory.character.internal.domain;

public class InvalidCharacterInputException extends RuntimeException {
    public InvalidCharacterInputException(String message) {
        super(message);
    }
}
