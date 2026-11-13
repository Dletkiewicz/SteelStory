package com.steelstory.identity.internal.application;

public record AuthenticateAccountCommand(String email, String password) {
    @Override
    public String toString() {
        return "AuthenticateAccountCommand[REDACTED]";
    }
}
