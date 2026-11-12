package com.steelstory.identity.internal.infrastructure;

import com.steelstory.identity.internal.application.PasswordHashing;
import com.steelstory.identity.internal.domain.PasswordHash;
import java.util.UUID;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class Argon2PasswordHashing implements PasswordHashing {
    // OWASP minimum: 19 MiB, 2 iterations, parallelism 1; random 16-byte salt, 32-byte output.
    private final Argon2PasswordEncoder passwordEncoder = new Argon2PasswordEncoder(16, 32, 1, 19456, 2);
    private final PasswordHash dummyHash = hash(UUID.randomUUID().toString());

    @Override
    public PasswordHash hash(String password) {
        return new PasswordHash(passwordEncoder.encode(password));
    }

    @Override
    public boolean matches(String password, PasswordHash hash) {
        return passwordEncoder.matches(password, hash.value());
    }

    @Override
    public void performDummyVerification(String password) {
        passwordEncoder.matches(password, dummyHash.value());
    }
}
