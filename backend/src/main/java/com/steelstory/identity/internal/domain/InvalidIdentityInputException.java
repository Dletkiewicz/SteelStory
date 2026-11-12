package com.steelstory.identity.internal.domain;

public class InvalidIdentityInputException extends RuntimeException {
    public InvalidIdentityInputException(String message) {
        super(message);
    }
}
