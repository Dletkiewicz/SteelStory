package com.steelstory.character.internal.infrastructure;

import com.steelstory.character.internal.application.CharacterRepository;
import com.steelstory.character.internal.domain.AppearanceGender;
import com.steelstory.character.internal.domain.CharacterAttributes;
import com.steelstory.character.internal.domain.CharacterClass;
import com.steelstory.character.internal.domain.CharacterId;
import com.steelstory.character.internal.domain.CharacterName;
import com.steelstory.character.internal.domain.PlayerCharacter;
import com.steelstory.identity.api.AccountId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCharacterRepository implements CharacterRepository {
    private final JdbcClient jdbcClient;

    public JdbcCharacterRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void lockAccount(AccountId accountId) {
        jdbcClient.sql("""
                INSERT INTO steelstory.character_account_guards (account_id) VALUES (:accountId)
                ON CONFLICT (account_id) DO NOTHING
                """).param("accountId", accountId.value()).update();
        jdbcClient.sql("""
                SELECT account_id FROM steelstory.character_account_guards
                WHERE account_id = :accountId FOR UPDATE
                """).param("accountId", accountId.value()).query(UUID.class).single();
    }

    @Override
    public int countOwned(AccountId accountId) {
        return jdbcClient.sql("""
                SELECT count(*) FROM steelstory.character_characters
                WHERE account_id = :accountId AND deleted_at IS NULL
                """)
                .param("accountId", accountId.value()).query(Integer.class).single();
    }

    @Override
    public boolean insert(PlayerCharacter character) {
        return jdbcClient.sql("""
                INSERT INTO steelstory.character_characters
                    (id, account_id, name, character_class, appearance_gender,
                     starting_strength, starting_dexterity, starting_intelligence, starting_constitution, starting_luck,
                     trained_strength, trained_dexterity, trained_intelligence, trained_constitution, trained_luck,
                     level, experience, unspent_training_points, created_at, updated_at)
                VALUES (:id, :accountId, :name, :characterClass, :appearanceGender,
                        :strength, :dexterity, :intelligence, :constitution, :luck,
                        0, 0, 0, 0, 0, 1, 0, 0, :createdAt, :updatedAt)
                ON CONFLICT (canonical_name) DO NOTHING
                """)
                .param("id", character.id().value())
                .param("accountId", character.accountId().value())
                .param("name", character.name().value())
                .param("characterClass", character.characterClass().name())
                .param("appearanceGender", character.appearanceGender().name())
                .param("strength", character.startingAttributes().strength())
                .param("dexterity", character.startingAttributes().dexterity())
                .param("intelligence", character.startingAttributes().intelligence())
                .param("constitution", character.startingAttributes().constitution())
                .param("luck", character.startingAttributes().luck())
                .param("createdAt", character.createdAt().atOffset(ZoneOffset.UTC))
                .param("updatedAt", character.updatedAt().atOffset(ZoneOffset.UTC))
                .update() == 1;
    }

    @Override
    public List<PlayerCharacter> findOwned(AccountId accountId) {
        return jdbcClient.sql("""
                SELECT * FROM steelstory.character_characters
                WHERE account_id = :accountId AND deleted_at IS NULL
                ORDER BY created_at, id
                """).param("accountId", accountId.value()).query(this::mapCharacter).list();
    }

    @Override
    public Optional<PlayerCharacter> findOwned(AccountId accountId, CharacterId characterId) {
        return jdbcClient.sql("""
                SELECT * FROM steelstory.character_characters
                WHERE account_id = :accountId AND id = :id AND deleted_at IS NULL
                """).param("accountId", accountId.value()).param("id", characterId.value())
                .query(this::mapCharacter).optional();
    }

    @Override
    public boolean markDeleted(PlayerCharacter character) {
        return jdbcClient.sql("""
                UPDATE steelstory.character_characters SET deleted_at = :deletedAt, updated_at = :deletedAt
                WHERE account_id = :accountId AND id = :id AND deleted_at IS NULL
                """).param("accountId", character.accountId().value()).param("id", character.id().value())
                .param("deletedAt", character.deletedAt().atOffset(ZoneOffset.UTC)).update() == 1;
    }

    @Override
    public Optional<PlayerCharacter> findOwnedForUpdate(AccountId accountId, CharacterId characterId) {
        return jdbcClient.sql("""
                SELECT * FROM steelstory.character_characters
                WHERE account_id = :accountId AND id = :id AND deleted_at IS NULL FOR UPDATE
                """).param("accountId", accountId.value()).param("id", characterId.value())
                .query(this::mapCharacter).optional();
    }

    @Override
    public void saveProgression(PlayerCharacter character) {
        int updated = jdbcClient.sql("""
                UPDATE steelstory.character_characters
                SET experience = :experience, level = :level, unspent_training_points = :points, updated_at = :updatedAt
                WHERE account_id = :accountId AND id = :id AND deleted_at IS NULL
                """).param("accountId", character.accountId().value()).param("id", character.id().value())
                .param("experience", character.experience()).param("level", character.level())
                .param("points", character.unspentTrainingPoints())
                .param("updatedAt", character.updatedAt().atOffset(ZoneOffset.UTC)).update();
        if (updated != 1) {
            throw new IllegalStateException("Locked character could not be updated.");
        }
    }

    private PlayerCharacter mapCharacter(ResultSet row, int rowNumber) throws SQLException {
        return PlayerCharacter.restore(new CharacterId(row.getObject("id", UUID.class)),
                new AccountId(row.getObject("account_id", UUID.class)), new CharacterName(row.getString("name")),
                CharacterClass.parse(row.getString("character_class")),
                AppearanceGender.parse(row.getString("appearance_gender")),
                attributes(row, "starting_"), attributes(row, "trained_"),
                row.getInt("level"), row.getLong("experience"), row.getInt("unspent_training_points"),
                row.getObject("created_at", OffsetDateTime.class).toInstant(),
                row.getObject("updated_at", OffsetDateTime.class).toInstant());
    }

    private CharacterAttributes attributes(ResultSet row, String prefix) throws SQLException {
        return new CharacterAttributes(row.getInt(prefix + "strength"), row.getInt(prefix + "dexterity"),
                row.getInt(prefix + "intelligence"), row.getInt(prefix + "constitution"), row.getInt(prefix + "luck"));
    }
}
