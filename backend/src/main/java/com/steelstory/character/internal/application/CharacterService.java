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
                command.name(), command.characterClass(), command.appearanceGender(), command.allocation(), now);
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

    @Transactional
    public void deleteOwn(AccountId accountId, CharacterId characterId) {
        characterRepository.lockAccount(accountId);
        requireActiveAccount(accountId);
        PlayerCharacter character = characterRepository.findOwnedForUpdate(accountId, characterId)
                .orElseThrow(CharacterNotFoundException::new);
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        if (!characterRepository.markDeleted(character.delete(now))) {
            throw new CharacterNotFoundException();
        }
    }

    /** Trusted server-side entry point. Each call is a new reward, not an idempotent event delivery. */
    @Transactional
    public ExperienceAwardResult awardExperience(AccountId accountId, CharacterId characterId, long amount) {
        characterRepository.lockAccount(accountId);
        requireActiveAccount(accountId);
        PlayerCharacter previous = characterRepository.findOwnedForUpdate(accountId, characterId)
                .orElseThrow(CharacterNotFoundException::new);
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        PlayerCharacter character = previous.gainExperience(amount, now);
        if (character != previous) {
            characterRepository.saveProgression(character);
        }

        return new ExperienceAwardResult(previous.level(), character.experience() - previous.experience(),
                character.unspentTrainingPoints() - previous.unspentTrainingPoints(), character);
    }

    private void requireActiveAccount(AccountId accountId) {
        if (!accountAccess.isActive(accountId)) {
            throw new InactiveAccountException();
        }
    }
}
