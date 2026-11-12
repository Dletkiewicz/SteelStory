package com.steelstory.identity.internal.domain;

public record PasswordHash(String value) {
    public PasswordHash {
        if (value == null || value.length() > 255 || !value.startsWith("$argon2id$v=19$")) {
            throw new IllegalArgumentException("An Argon2id password hash is required.");
        }
    }

    @Override
    public String toString() {
        return "PasswordHash[REDACTED]";
    }
}
