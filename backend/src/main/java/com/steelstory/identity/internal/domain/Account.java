package com.steelstory.identity.internal.domain;

import com.steelstory.identity.api.AccountId;
import java.time.Instant;
import java.util.Objects;

public final class Account {
    private final AccountId id;
    private final EmailAddress email;
    private final PasswordHash passwordHash;
    private final AccountStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Account(AccountId id, EmailAddress email, PasswordHash passwordHash, AccountStatus status,
            Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.email = Objects.requireNonNull(email);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);

        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Account update precedes creation.");
        }
    }

    public static Account register(AccountId id, EmailAddress email, PasswordHash hash, Instant now) {
        return new Account(id, email, hash, AccountStatus.ACTIVE, now, now);
    }

    public static Account restore(AccountId id, EmailAddress email, PasswordHash hash, AccountStatus status,
            Instant createdAt, Instant updatedAt) {
        return new Account(id, email, hash, status, createdAt, updatedAt);
    }

    public boolean canAuthenticate() {
        return status == AccountStatus.ACTIVE;
    }

    public AccountId id() {
        return id;
    }

    public EmailAddress email() {
        return email;
    }

    public PasswordHash passwordHash() {
        return passwordHash;
    }

    public AccountStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
