package com.steelstory.character.internal.domain;

import com.steelstory.identity.api.AccountId;
import java.time.Instant;
import java.util.Objects;

public final class PlayerCharacter {
    private final CharacterId id;
    private final AccountId accountId;
    private final CharacterName name;
    private final CharacterClass characterClass;
    private final CharacterAttributes startingAttributes;
    private final CharacterAttributes trainedAttributes;
    private final int level;
    private final long experience;
    private final int unspentTrainingPoints;
    private final Instant createdAt;
    private final Instant updatedAt;

    private PlayerCharacter(CharacterId id, AccountId accountId, CharacterName name, CharacterClass characterClass,
            CharacterAttributes startingAttributes, CharacterAttributes trainedAttributes, int level, long experience,
            int unspentTrainingPoints, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.accountId = Objects.requireNonNull(accountId);
        this.name = Objects.requireNonNull(name);
        this.characterClass = Objects.requireNonNull(characterClass);
        this.startingAttributes = Objects.requireNonNull(startingAttributes);
        this.trainedAttributes = Objects.requireNonNull(trainedAttributes);
        this.level = level;
        this.experience = experience;
        this.unspentTrainingPoints = unspentTrainingPoints;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);

        if (level < 1 || experience < 0 || unspentTrainingPoints < 0 || updatedAt.isBefore(createdAt)) {
            throw new InvalidCharacterInputException("Invalid character progression or timestamps.");
        }

        if (startingAttributes.total() != 35
                || startingAttributes.strength() < 5
                || startingAttributes.dexterity() < 5
                || startingAttributes.intelligence() < 5
                || startingAttributes.constitution() < 5
                || startingAttributes.luck() < 5) {
            throw new InvalidCharacterInputException(
                    "Starting attributes must include base 5 and exactly 10 extra points.");
        }
    }

    public static PlayerCharacter create(CharacterId id, AccountId accountId, CharacterName name,
            CharacterClass characterClass, StartingAllocation allocation, Instant now) {
        return new PlayerCharacter(id, accountId, name, characterClass, allocation.startingAttributes(),
                new CharacterAttributes(0, 0, 0, 0, 0), 1, 0, 0, now, now);
    }

    public static PlayerCharacter restore(CharacterId id, AccountId accountId, CharacterName name,
            CharacterClass characterClass, CharacterAttributes startingAttributes,
            CharacterAttributes trainedAttributes,
            int level, long experience, int unspentTrainingPoints, Instant createdAt, Instant updatedAt) {
        return new PlayerCharacter(id, accountId, name, characterClass, startingAttributes, trainedAttributes,
                level, experience, unspentTrainingPoints, createdAt, updatedAt);
    }

    public CharacterId id() {
        return id;
    }

    public AccountId accountId() {
        return accountId;
    }

    public CharacterName name() {
        return name;
    }

    public CharacterClass characterClass() {
        return characterClass;
    }

    public CharacterAttributes startingAttributes() {
        return startingAttributes;
    }

    public CharacterAttributes trainedAttributes() {
        return trainedAttributes;
    }

    public int level() {
        return level;
    }

    public long experience() {
        return experience;
    }

    public int unspentTrainingPoints() {
        return unspentTrainingPoints;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
