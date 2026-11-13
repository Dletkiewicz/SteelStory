package com.steelstory.character.internal.application;

import com.steelstory.character.internal.domain.CharacterId;
import com.steelstory.character.internal.domain.PlayerCharacter;
import com.steelstory.identity.api.AccountId;
import java.util.List;
import java.util.Optional;

// Reads and counts include only active characters; retained deleted rows are never playable.
public interface CharacterRepository {
    void lockAccount(AccountId accountId);

    int countOwned(AccountId accountId);

    boolean insert(PlayerCharacter character);

    boolean markDeleted(PlayerCharacter character);

    Optional<PlayerCharacter> findOwnedForUpdate(AccountId accountId, CharacterId characterId);

    void saveProgression(PlayerCharacter character);

    List<PlayerCharacter> findOwned(AccountId accountId);

    Optional<PlayerCharacter> findOwned(AccountId accountId, CharacterId characterId);
}
