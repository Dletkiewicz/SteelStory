package com.steelstory.identity;

import com.steelstory.identity.internal.domain.EmailAddress;
import com.steelstory.identity.internal.domain.InvalidIdentityInputException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class IdentityMigrationIT {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse(
            "postgres:18.6@sha256:e3d24f6906d0760bf4da0dae872415391ededefad23fd471c47dc53b0ddf3c42")
            .asCompatibleSubstituteFor("postgres"));

    @Test
    void upgradesFoundationWithoutChangingItsChecksumOrExistingDataAndEnforcesConstraints() throws Exception {
        Flyway foundation = configuration().target("0001").load();
        assertThat(foundation.migrate().migrationsExecuted).isEqualTo(1);
        Integer checksum = foundation.info().current().getChecksum();
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE steelstory.foundation_upgrade_probe (value TEXT NOT NULL)");
            statement.execute("INSERT INTO steelstory.foundation_upgrade_probe VALUES ('preserved')");
        }

        Flyway legacy = configuration().target("0003").load();
        assertThat(legacy.migrate().migrationsExecuted).isEqualTo(2);
        List<Integer> originalChecksums = Arrays.stream(legacy.info().applied())
                .map(MigrationInfo::getChecksum).toList();

        Flyway current = configuration().load();
        assertLegacyEmailGapsAndSafeMigrationFailure(current);
        assertThat(current.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(Arrays.stream(current.info().applied()).limit(3).map(MigrationInfo::getChecksum).toList())
                .isEqualTo(originalChecksums);
        assertThat(current.info().applied()[0].getChecksum()).isEqualTo(checksum);
        assertThat(current.validateWithResult().validationSuccessful).isTrue();
        assertThat(current.migrate().migrationsExecuted).isZero();

        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            try (ResultSet rows = statement.executeQuery("SELECT value FROM steelstory.foundation_upgrade_probe")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("preserved");
            }

            insertAccount(connection, "valid@example.com", "ACTIVE", "now()", "now()");
            assertConstraint(connection, "valid@example.com", "ACTIVE", "now()", "now()", "23505");
            assertConstraint(connection, "UPPER@example.com", "ACTIVE", "now()", "now()", "23514");
            assertConstraint(connection, " spaced@example.com ", "ACTIVE", "now()", "now()", "23514");
            assertConstraint(connection, "state@example.com", "ADMIN", "now()", "now()", "23514");
            assertConstraint(connection, "time@example.com", "ACTIVE", "now()", "'2000-01-01'", "23514");
            assertConstraint(connection, "a".repeat(255) + "@example.com", "ACTIVE", "now()", "now()", "22001");
            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO steelstory.identity_accounts
                        (id, email, password_hash, status, created_at, updated_at)
                    VALUES (gen_random_uuid(), NULL, '$argon2id$v=19$test', 'ACTIVE', now(), now())
                    """)).isInstanceOfSatisfying(SQLException.class,
                            exception -> assertThat(exception.getSQLState()).isEqualTo("23502"));

            assertEmailInvariants(connection);
        }
    }

    private static void assertLegacyEmailGapsAndSafeMigrationFailure(Flyway current) throws SQLException {
        List<String> invalidEmails = List.of("foo@@example.com", "foo@example..com", "foo@.com");
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            for (String email : invalidEmails) {
                assertThatThrownBy(() -> new EmailAddress(email)).isInstanceOf(InvalidIdentityInputException.class);
                insertAccount(connection, email, "ACTIVE", "now()", "now()");
            }

            assertThatThrownBy(current::migrate).isInstanceOf(FlywayException.class)
                    .hasRootCauseInstanceOf(SQLException.class);
            assertThat(current.info().current().getVersion().toString()).isEqualTo("0003");
            try (ResultSet rows = statement.executeQuery("SELECT email FROM steelstory.identity_accounts")) {
                int count = 0;
                while (rows.next()) {
                    assertThat(invalidEmails).contains(rows.getString(1));
                    count++;
                }

                assertThat(count).isEqualTo(invalidEmails.size());
            }

            // Explicitly repair only synthetic fixtures in this isolated Testcontainers database.
            // The migration itself must neither delete nor rewrite existing account identities.
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE steelstory.identity_accounts SET email = ? WHERE email = ?")) {
                for (int index = 0; index < invalidEmails.size(); index++) {
                    update.setString(1, "repaired-" + index + "@example.com");
                    update.setString(2, invalidEmails.get(index));
                    assertThat(update.executeUpdate()).isEqualTo(1);
                }
            }
        }
    }

    private static void assertEmailInvariants(Connection connection) throws SQLException {
        for (String email : List.of("foo@@example.com", "foo@example..com", "foo@.com", "a@b@c.com",
                "@example.com", "a@example", "a@example.com.", "a@-example.com", "a@example-.com",
                "a@sub.-example.com", "a@sub-.example.com", ".a@example.com", "a.@example.com", "a..b@example.com",
                "a:b@example.com", "a".repeat(65) + "@example.com", "a@" + "b".repeat(64) + ".com")) {
            assertThatThrownBy(() -> new EmailAddress(email)).isInstanceOf(InvalidIdentityInputException.class);
            assertConstraint(connection, email, "ACTIVE", "now()", "now()", "23514");
        }

        String longestDomain = "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(61);
        for (String email : List.of("first.last+tag@sub-domain.example.com", "a@b.c",
                "!#$%&'*+/=?^_`{|}~-@example.com", "a".repeat(64) + "@" + longestDomain)) {
            assertThat(new EmailAddress(email).value()).isEqualTo(email);
            insertAccount(connection, email, "ACTIVE", "now()", "now()");
        }

        try (Statement statement = connection.createStatement()) {
            assertThatThrownBy(() -> statement.executeUpdate(
                    "UPDATE steelstory.identity_accounts SET email = 'foo@@example.com' "
                            + "WHERE email = 'valid@example.com'"))
                    .isInstanceOfSatisfying(SQLException.class,
                            exception -> assertThat(exception.getSQLState()).isEqualTo("23514"));
            try (ResultSet rows = statement.executeQuery(
                    "SELECT count(*) FROM steelstory.identity_accounts WHERE email LIKE 'repaired-%@example.com'")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getInt(1)).isEqualTo(3);
            }
        }
    }

    private static void assertConstraint(Connection connection, String email, String status,
            String created, String updated, String sqlState) {
        assertThatThrownBy(() -> insertAccount(connection, email, status, created, updated))
                .isInstanceOfSatisfying(SQLException.class,
                        exception -> assertThat(exception.getSQLState()).isEqualTo(sqlState));
    }

    private static void insertAccount(Connection connection, String email, String status,
            String created, String updated) throws SQLException {
        // Only test-owned timestamp expressions are interpolated; values remain bound.
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO steelstory.identity_accounts
                    (id, email, password_hash, status, created_at, updated_at)
                VALUES (gen_random_uuid(), ?, '$argon2id$v=19$test', ?, %s, %s)
                """.formatted(created, updated))) {
            statement.setString(1, email);
            statement.setString(2, status);
            statement.executeUpdate();
        }
    }

    private static FluentConfiguration configuration() {
        return Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .cleanDisabled(true);
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
