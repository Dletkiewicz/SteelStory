package com.steelstory.character.internal.domain;

import com.steelstory.identity.api.AccountId;
import java.time.Instant;
import java.util.Objects;

public final class PlayerCharacter {
    private static final int TRAINING_POINTS_PER_LEVEL = 3;
    private final CharacterId id;
    private final AccountId accountId;
    private final CharacterName name;
    private final CharacterClass characterClass;
    private final AppearanceGender appearanceGender;
    private final CharacterAttributes startingAttributes;
    private final CharacterAttributes trainedAttributes;
    private final int level;
    private final long experience;
    private final int unspentTrainingPoints;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant deletedAt;

    private PlayerCharacter(CharacterId id, AccountId accountId, CharacterName name, CharacterClass characterClass,
            AppearanceGender appearanceGender, CharacterAttributes startingAttributes,
            CharacterAttributes trainedAttributes, int level, long experience,
            int unspentTrainingPoints, Instant createdAt, Instant updatedAt, Instant deletedAt) {
        this.id = Objects.requireNonNull(id);
        this.accountId = Objects.requireNonNull(accountId);
        this.name = Objects.requireNonNull(name);
        this.characterClass = Objects.requireNonNull(characterClass);
        this.appearanceGender = Objects.requireNonNull(appearanceGender);
        this.startingAttributes = Objects.requireNonNull(startingAttributes);
        this.trainedAttributes = Objects.requireNonNull(trainedAttributes);
        this.level = level;
        this.experience = experience;
        this.unspentTrainingPoints = unspentTrainingPoints;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.deletedAt = deletedAt;

        if (ExperienceCurve.levelForTotalExperience(experience) != level
                || unspentTrainingPoints < 0 || unspentTrainingPoints > (level - 1) * TRAINING_POINTS_PER_LEVEL
                || updatedAt.isBefore(createdAt)) {
            throw new InvalidCharacterInputException("Invalid character progression or timestamps.");
        }

        if (deletedAt != null && !deletedAt.equals(updatedAt)) {
            throw new InvalidCharacterInputException("Deletion must be the final character update.");
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
            CharacterClass characterClass, AppearanceGender appearanceGender,
            StartingAllocation allocation, Instant now) {
        return new PlayerCharacter(id, accountId, name, characterClass, appearanceGender,
                allocation.startingAttributes(),
                new CharacterAttributes(0, 0, 0, 0, 0), 1, 0, 0, now, now, null);
    }

    public static PlayerCharacter restore(CharacterId id, AccountId accountId, CharacterName name,
            CharacterClass characterClass, AppearanceGender appearanceGender, CharacterAttributes startingAttributes,
            CharacterAttributes trainedAttributes,
            int level, long experience, int unspentTrainingPoints, Instant createdAt, Instant updatedAt) {
        return new PlayerCharacter(id, accountId, name, characterClass, appearanceGender,
                startingAttributes, trainedAttributes,
                level, experience, unspentTrainingPoints, createdAt, updatedAt, null);
    }

    public PlayerCharacter delete(Instant now) {
        Objects.requireNonNull(now);
        if (deletedAt != null || now.isBefore(updatedAt)) {
            throw new InvalidCharacterInputException("Character cannot be deleted in its current state.");
        }

        return new PlayerCharacter(id, accountId, name, characterClass, appearanceGender,
                startingAttributes, trainedAttributes,
                level, experience, unspentTrainingPoints, createdAt, now, now);
    }

    public PlayerCharacter gainExperience(long awardedExperience, Instant now) {
        Objects.requireNonNull(now);
        if (awardedExperience <= 0 || deletedAt != null || now.isBefore(updatedAt)) {
            throw new InvalidCharacterInputException("Experience requires a positive award and an active character.");
        }

        // Subtract before adding, so even Long.MAX_VALUE awards cannot overflow.
        long accepted = Math.min(awardedExperience, ExperienceCurve.MAXIMUM_EXPERIENCE - experience);
        if (accepted == 0) {
            return this;
        }

        long totalExperience = experience + accepted;
        int currentLevel = ExperienceCurve.levelForTotalExperience(totalExperience);
        int trainingPoints = Math.addExact(unspentTrainingPoints,
                (currentLevel - level) * TRAINING_POINTS_PER_LEVEL);
        return new PlayerCharacter(id, accountId, name, characterClass, appearanceGender,
                startingAttributes, trainedAttributes, currentLevel, totalExperience, trainingPoints,
                createdAt, now, null);
    }

    public Instant deletedAt() {
        return deletedAt;
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

    public AppearanceGender appearanceGender() {
        return appearanceGender;
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
