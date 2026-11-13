package com.steelstory.character.internal.application;

import com.steelstory.character.internal.domain.PlayerCharacter;

public record ExperienceAwardResult(int previousLevel, long acceptedExperience, int trainingPointsAwarded,
        PlayerCharacter character) {
    public int levelsGained() {
        return character.level() - previousLevel;
    }
}
