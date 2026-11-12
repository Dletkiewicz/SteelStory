package com.steelstory.identity.internal.application;

import com.steelstory.identity.api.AccountAccess;
import com.steelstory.identity.api.AccountId;
import com.steelstory.identity.internal.domain.Account;
import com.steelstory.identity.internal.domain.EmailAddress;
import com.steelstory.identity.internal.domain.PasswordHash;
import com.steelstory.identity.internal.domain.PasswordPolicy;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class IdentityService implements AccountAccess {
    private final AccountRepository accountRepository;
    private final PasswordHashing passwordHashing;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public IdentityService(AccountRepository accountRepository, PasswordHashing passwordHashing, Clock clock,
            PlatformTransactionManager platformTransactionManager) {
        this.accountRepository = accountRepository;
        this.passwordHashing = passwordHashing;
        this.clock = clock;
        this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
    }

    public Account register(String email, String password) {
        EmailAddress emailAddress = new EmailAddress(email);
        PasswordPolicy.validateNewPassword(password);

        PasswordHash passwordHash = passwordHashing.hash(password);
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        Account account = Account.register(new AccountId(UUID.randomUUID()), emailAddress, passwordHash, now);

        // The HTTP registration entrypoint has no surrounding transaction.
        // Keep expensive hashing above this short insert/commit boundary.
        transactionTemplate.executeWithoutResult(status -> {
            if (!accountRepository.insert(account)) {
                throw new EmailAlreadyRegisteredException();
            }
        });

        return account;
    }

    public Account authenticate(String email, String password) {
        EmailAddress emailAddress = new EmailAddress(email);
        PasswordPolicy.validateLoginInput(password);

        Account account = accountRepository.findByEmail(emailAddress).orElseThrow(() -> {
            passwordHashing.performDummyVerification(password);
            return new InvalidCredentialsException();
        });

        boolean passwordMatches = passwordHashing.matches(password, account.passwordHash());
        if (!passwordMatches || !account.canAuthenticate()) {
            throw new InvalidCredentialsException();
        }

        return account;
    }

    public Account currentAccount(AccountId accountId) {
        return accountRepository.findById(accountId)
                .filter(Account::canAuthenticate)
                .orElseThrow(InvalidCredentialsException::new);
    }

    @Override
    public boolean isActive(AccountId accountId) {
        return accountRepository.isActive(accountId);
    }
}
