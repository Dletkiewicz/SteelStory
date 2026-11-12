package com.steelstory.identity.api;

import java.util.Objects;
import java.util.UUID;

public record AccountId(UUID value) {
    public AccountId {
        Objects.requireNonNull(value, "Account ID is required");
    }
}
