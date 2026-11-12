package com.steelstory.identity.internal.application;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public static final String MESSAGE = "Email is already registered.";

    public EmailAlreadyRegisteredException() {
        super(MESSAGE);
    }
}
