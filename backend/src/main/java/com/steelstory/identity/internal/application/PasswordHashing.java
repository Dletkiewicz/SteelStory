package com.steelstory.identity.internal.application;

import com.steelstory.identity.internal.domain.PasswordHash;

public interface PasswordHashing {
    PasswordHash hash(String password);

    boolean matches(String password, PasswordHash hash);

    void performDummyVerification(String password);
}
