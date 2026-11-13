package com.steelstory.identity.internal.application;

public record RegisterAccountCommand(String email, String password) {
    @Override
    public String toString() {
        return "RegisterAccountCommand[REDACTED]";
    }
}
