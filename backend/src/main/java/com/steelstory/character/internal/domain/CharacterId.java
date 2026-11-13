package com.steelstory.character.internal.domain;

import java.util.Objects;
import java.util.UUID;

public record CharacterId(UUID value) {
    public CharacterId {
        Objects.requireNonNull(value, "Character ID is required.");
    }
}
