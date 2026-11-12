package com.steelstory.character.internal.application;

import com.steelstory.character.internal.domain.CharacterId;
import com.steelstory.character.internal.domain.PlayerCharacter;
import com.steelstory.identity.api.AccountAccess;
import com.steelstory.identity.api.AccountId;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CharacterService {
    private final CharacterRepository characterRepository;
    private final AccountAccess accountAccess;
    private final Clock clock;

    public CharacterService(CharacterRepository characterRepository, AccountAccess accountAccess, Clock clock) {
        this.characterRepository = characterRepository;
        this.accountAccess = accountAccess;
        this.clock = clock;
    }

    @Transactional
    public PlayerCharacter create(AccountId accountId, CreateCharacterCommand command) {
        characterRepository.lockAccount(accountId);
        requireActiveAccount(accountId);
        if (characterRepository.countOwned(accountId) >= 3) {
            throw new CharacterLimitReachedException();
        }

        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        PlayerCharacter character = PlayerCharacter.create(new CharacterId(UUID.randomUUID()), accountId,
                command.name(), command.characterClass(), command.allocation(), now);
        if (!characterRepository.insert(character)) {
            throw new CharacterNameTakenException();
        }

        return character;
    }

    public List<PlayerCharacter> listOwn(AccountId accountId) {
        requireActiveAccount(accountId);
        return characterRepository.findOwned(accountId);
    }

    public PlayerCharacter getOwn(AccountId accountId, CharacterId characterId) {
        requireActiveAccount(accountId);
        return characterRepository.findOwned(accountId, characterId).orElseThrow(CharacterNotFoundException::new);
    }

    private void requireActiveAccount(AccountId accountId) {
        if (!accountAccess.isActive(accountId)) {
            throw new InactiveAccountException();
        }
    }
}
