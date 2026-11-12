package com.steelstory.character.internal.application;

import com.steelstory.character.internal.domain.CharacterId;
import com.steelstory.character.internal.domain.PlayerCharacter;
import com.steelstory.identity.api.AccountId;
import java.util.List;
import java.util.Optional;

public interface CharacterRepository {
    void lockAccount(AccountId accountId);

    int countOwned(AccountId accountId);

    boolean insert(PlayerCharacter character);

    List<PlayerCharacter> findOwned(AccountId accountId);

    Optional<PlayerCharacter> findOwned(AccountId accountId, CharacterId characterId);
}
