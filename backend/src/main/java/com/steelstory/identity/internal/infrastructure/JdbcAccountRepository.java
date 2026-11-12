package com.steelstory.identity.internal.infrastructure;

import com.steelstory.identity.api.AccountId;
import com.steelstory.identity.internal.application.AccountRepository;
import com.steelstory.identity.internal.domain.Account;
import com.steelstory.identity.internal.domain.AccountStatus;
import com.steelstory.identity.internal.domain.EmailAddress;
import com.steelstory.identity.internal.domain.PasswordHash;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAccountRepository implements AccountRepository {
    private final JdbcClient jdbcClient;

    public JdbcAccountRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public boolean insert(Account account) {
        return jdbcClient.sql("""
                INSERT INTO steelstory.identity_accounts
                    (id, email, password_hash, status, created_at, updated_at)
                VALUES (:id, :email, :hash, :status, :created, :updated)
                ON CONFLICT (email) DO NOTHING
                """)
                .param("id", account.id().value())
                .param("email", account.email().value())
                .param("hash", account.passwordHash().value())
                .param("status", account.status().name())
                .param("created", account.createdAt().atOffset(ZoneOffset.UTC))
                .param("updated", account.updatedAt().atOffset(ZoneOffset.UTC))
                .update() == 1;
    }

    @Override
    public Optional<Account> findByEmail(EmailAddress email) {
        return jdbcClient.sql("SELECT * FROM steelstory.identity_accounts WHERE email = :email")
                .param("email", email.value()).query(this::mapAccount).optional();
    }

    @Override
    public Optional<Account> findById(AccountId id) {
        return jdbcClient.sql("SELECT * FROM steelstory.identity_accounts WHERE id = :id")
                .param("id", id.value()).query(this::mapAccount).optional();
    }

    @Override
    public boolean isActive(AccountId id) {
        return jdbcClient.sql("""
                SELECT EXISTS (SELECT 1 FROM steelstory.identity_accounts WHERE id = :id AND status = 'ACTIVE')
                """)
                .param("id", id.value()).query(Boolean.class).single();
    }

    private Account mapAccount(ResultSet row, int rowNumber) throws SQLException {
        return Account.restore(new AccountId(row.getObject("id", UUID.class)),
                new EmailAddress(row.getString("email")), new PasswordHash(row.getString("password_hash")),
                AccountStatus.valueOf(row.getString("status")),
                row.getObject("created_at", OffsetDateTime.class).toInstant(),
                row.getObject("updated_at", OffsetDateTime.class).toInstant());
    }
}
