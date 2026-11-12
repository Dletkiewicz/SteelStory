package com.steelstory.identity.internal.application;

public class InvalidCredentialsException extends RuntimeException {
    public static final String MESSAGE = "Invalid email or password.";

    public InvalidCredentialsException() {
        super(MESSAGE);
    }
}
