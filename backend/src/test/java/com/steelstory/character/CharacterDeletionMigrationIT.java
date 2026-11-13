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
class CharacterDeletionMigrationIT {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse(
            "postgres:18.6@sha256:e3d24f6906d0760bf4da0dae872415391ededefad23fd471c47dc53b0ddf3c42")
            .asCompatibleSubstituteFor("postgres"));

    @Test
    void upgradesV5WithoutDeletingCharactersOrReleasingNames() {
        Flyway previous = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .target("0005").load();
        previous.migrate();
        List<Integer> checksums = Arrays.stream(previous.info().applied()).map(MigrationInfo::getChecksum).toList();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        UUID accountId = UUID.randomUUID();
        UUID characterId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO steelstory.character_account_guards VALUES (?)", accountId);
        jdbcTemplate.update("""
                INSERT INTO steelstory.character_characters
                    (id, account_id, name, character_class,
                     starting_strength, starting_dexterity, starting_intelligence, starting_constitution, starting_luck,
                     trained_strength, trained_dexterity, trained_intelligence, trained_constitution, trained_luck,
                     level, experience, unspent_training_points, created_at, updated_at)
                VALUES (?, ?, 'Retained Hero', 'MAGE', 15, 5, 5, 5, 5, 0, 0, 0, 0, 0, 1, 0, 0, now(), now())
                """, characterId, accountId);
        String original = jdbcTemplate.queryForObject(
                "SELECT row_to_json(c)::text FROM steelstory.character_characters c WHERE id = ?",
                String.class, characterId);

        Flyway current = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .target("0006").load();
        assertThat(current.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(Arrays.stream(current.info().applied()).limit(5).map(MigrationInfo::getChecksum).toList())
                .isEqualTo(checksums);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT deleted_at IS NULL FROM steelstory.character_characters WHERE id = ?",
                Boolean.class, characterId)).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT row_to_json(c)::text FROM steelstory.character_characters c WHERE id = ?",
                String.class, characterId)).isEqualTo(original.substring(0, original.length() - 1)
                        + ",\"deleted_at\":null}");
        assertThatThrownBy(() -> jdbcTemplate.update("""
                UPDATE steelstory.character_characters SET deleted_at = created_at - interval '1 second' WHERE id = ?
                """, characterId)).isInstanceOf(DataIntegrityViolationException.class);
        jdbcTemplate.update("""
                UPDATE steelstory.character_characters SET deleted_at = now(), updated_at = now() WHERE id = ?
                """, characterId);
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO steelstory.character_characters
                    (id, account_id, name, character_class,
                     starting_strength, starting_dexterity, starting_intelligence, starting_constitution, starting_luck,
                     trained_strength, trained_dexterity, trained_intelligence, trained_constitution, trained_luck,
                     level, experience, unspent_training_points, created_at, updated_at)
                SELECT ?, account_id, 'RETAINED HERO', character_class,
                       starting_strength, starting_dexterity, starting_intelligence,
                       starting_constitution, starting_luck,
                       trained_strength, trained_dexterity, trained_intelligence, trained_constitution, trained_luck,
                       level, experience, unspent_training_points, created_at, updated_at
                FROM steelstory.character_characters WHERE id = ?
                """, UUID.randomUUID(), characterId)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(current.validateWithResult().validationSuccessful).isTrue();
        assertThat(current.migrate().migrationsExecuted).isZero();
    }
}
