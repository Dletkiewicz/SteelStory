package com.steelstory.character.internal.application;

public class InactiveAccountException extends RuntimeException {
    public InactiveAccountException() {
        super("An active account is required.");
    }
}
