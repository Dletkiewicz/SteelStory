package com.steelstory.identity.internal.application;

import com.steelstory.identity.api.AccountId;
import com.steelstory.identity.internal.domain.Account;
import com.steelstory.identity.internal.domain.EmailAddress;
import java.util.Optional;

public interface AccountRepository {
    boolean insert(Account account);

    Optional<Account> findByEmail(EmailAddress email);

    Optional<Account> findById(AccountId id);

    boolean isActive(AccountId id);
}
