package com.steelstory.character;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class CharacterAppearanceMigrationIT {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse(
            "postgres:18.6@sha256:e3d24f6906d0760bf4da0dae872415391ededefad23fd471c47dc53b0ddf3c42")
            .asCompatibleSubstituteFor("postgres"));

    @Test
    void upgradesV6PreservingActiveAndDeletedHistoryAndEnforcesAppearanceValues() {
        Flyway previous = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .target("0006").load();
        previous.migrate();
        List<Integer> checksums = Arrays.stream(previous.info().applied()).map(MigrationInfo::getChecksum).toList();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        UUID accountId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO steelstory.character_account_guards VALUES (?)", accountId);
        for (String characterClass : List.of("WARRIOR", "ARCHER", "MAGE")) {
            for (boolean deleted : List.of(false, true)) {
                jdbcTemplate.update("""
                        INSERT INTO steelstory.character_characters
                            (id, account_id, name, character_class,
                             starting_strength, starting_dexterity, starting_intelligence,
                             starting_constitution, starting_luck,
                             trained_strength, trained_dexterity, trained_intelligence,
                             trained_constitution, trained_luck,
                             level, experience, unspent_training_points, created_at, updated_at, deleted_at)
                        VALUES (?, ?, ?, ?, 15, 5, 5, 5, 5, 0, 0, 0, 0, 0, 1, 0, 0,
                                now(), now(), CASE WHEN ? THEN now() ELSE NULL END)
                        """, UUID.randomUUID(), accountId, characterClass + (deleted ? " Retired" : " Active"),
                        characterClass, deleted);
            }
        }

        List<String> original = jdbcTemplate.queryForList(
                "SELECT to_jsonb(c)::text FROM steelstory.character_characters c ORDER BY id", String.class);
        Flyway current = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()).load();
        assertThat(current.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(Arrays.stream(current.info().applied()).limit(6).map(MigrationInfo::getChecksum).toList())
                .isEqualTo(checksums);
        assertThat(jdbcTemplate.queryForList("""
                SELECT (to_jsonb(c) - 'appearance_gender')::text
                FROM steelstory.character_characters c ORDER BY id
                """, String.class)).isEqualTo(original);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM steelstory.character_characters
                WHERE appearance_gender = CASE WHEN character_class = 'ARCHER' THEN 'FEMALE' ELSE 'MALE' END
                """, Integer.class)).isEqualTo(6);
        for (String value : List.of("'OTHER'", "'male'", "NULL")) {
            assertThatThrownBy(() -> jdbcTemplate.update(
                    "UPDATE steelstory.character_characters SET appearance_gender = " + value))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        assertThat(current.validateWithResult().validationSuccessful).isTrue();
        assertThat(current.migrate().migrationsExecuted).isZero();
    }
}
