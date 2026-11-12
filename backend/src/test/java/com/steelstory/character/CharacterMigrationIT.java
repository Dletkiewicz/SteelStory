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
class CharacterMigrationIT {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse(
            "postgres:18.6@sha256:e3d24f6906d0760bf4da0dae872415391ededefad23fd471c47dc53b0ddf3c42")
            .asCompatibleSubstituteFor("postgres"));

    @Test
    void upgradesV4PreservingHistoryAndEnforcesCharacterConstraints() {
        Flyway previous = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .target("0004").load();
        previous.migrate();
        List<Integer> checksums = Arrays.stream(previous.info().applied()).map(MigrationInfo::getChecksum).toList();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        jdbcTemplate.execute("CREATE TABLE steelstory.character_upgrade_probe (value INTEGER NOT NULL)");
        jdbcTemplate.update("INSERT INTO steelstory.character_upgrade_probe VALUES (42)");

        Flyway current = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()).load();
        assertThat(current.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(Arrays.stream(current.info().applied()).limit(4).map(MigrationInfo::getChecksum).toList())
                .isEqualTo(checksums);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT value FROM steelstory.character_upgrade_probe", Integer.class)).isEqualTo(42);
        assertThat(current.validateWithResult().validationSuccessful).isTrue();
        assertThat(current.migrate().migrationsExecuted).isZero();

        UUID accountId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO steelstory.character_account_guards VALUES (?)", accountId);
        UUID characterId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO steelstory.character_characters
                    (id, account_id, name, character_class,
                     starting_strength, starting_dexterity, starting_intelligence, starting_constitution, starting_luck,
                     trained_strength, trained_dexterity, trained_intelligence, trained_constitution, trained_luck,
                     level, experience, unspent_training_points, created_at, updated_at)
                VALUES (?, ?, 'Dark Knight', 'MAGE', 15, 5, 5, 5, 5, 0, 0, 0, 0, 0, 1, 0, 0, now(), now())
                """, characterId, accountId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT canonical_name FROM steelstory.character_characters WHERE id = ?",
                String.class, characterId)).isEqualTo("dark knight");

        for (String assignment : List.of("name = 'bad  name'", "name = 'bad_name'", "name = 'ab'",
                "name = 'abcdefghijklmnopqrstu'", "name = 'Dąrek'", "character_class = 'ROGUE'",
                "starting_strength = 14", "starting_dexterity = 4", "trained_luck = -1",
                "level = 0", "experience = -1", "unspent_training_points = -1",
                "updated_at = created_at - interval '1 second'")) {
            assertThatThrownBy(() -> jdbcTemplate.update(
                    "UPDATE steelstory.character_characters SET " + assignment + " WHERE id = ?", characterId))
                    .as(assignment).isInstanceOf(DataIntegrityViolationException.class);
        }

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO steelstory.character_characters
                    (id, account_id, name, character_class,
                     starting_strength, starting_dexterity, starting_intelligence, starting_constitution, starting_luck,
                     trained_strength, trained_dexterity, trained_intelligence, trained_constitution, trained_luck,
                     level, experience, unspent_training_points, created_at, updated_at)
                SELECT ?, account_id, 'DARK KNIGHT', character_class,
                       starting_strength, starting_dexterity, starting_intelligence,
                       starting_constitution, starting_luck,
                       trained_strength, trained_dexterity, trained_intelligence, trained_constitution, trained_luck,
                       level, experience, unspent_training_points, created_at, updated_at
                FROM steelstory.character_characters WHERE id = ?
                """, UUID.randomUUID(), characterId)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
