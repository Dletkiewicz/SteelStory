package com.steelstory.identity.internal.infrastructure;

import com.steelstory.identity.internal.domain.PasswordHash;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Argon2PasswordHashingTest {
    @Test
    void hashesWithIndependentSaltsAndPreservesAllPasswordCharacters() {
        Argon2PasswordHashing hashing = new Argon2PasswordHashing();
        String password = "  synthetic password 😀  ";
        PasswordHash first = hashing.hash(password);
        PasswordHash second = hashing.hash(password);
        assertThat(first.value()).startsWith("$argon2id$v=19$m=19456,t=2,p=1$");
        assertThat(first).isNotEqualTo(second);
        assertThat(hashing.matches(password, first)).isTrue();
        assertThat(hashing.matches(password.strip(), first)).isFalse();
        assertThat(hashing.matches("a different password", first)).isFalse();
    }
}
