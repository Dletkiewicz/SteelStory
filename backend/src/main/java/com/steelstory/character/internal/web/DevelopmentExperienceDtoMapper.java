package com.steelstory.character.internal.web;

import com.steelstory.character.internal.application.ExperienceAwardResult;
import com.steelstory.character.internal.domain.InvalidCharacterInputException;
import com.steelstory.character.internal.web.dto.GrantExperienceRequestDto;
import com.steelstory.character.internal.web.dto.GrantExperienceResponseDto;

final class DevelopmentExperienceDtoMapper {
    private DevelopmentExperienceDtoMapper() {
    }

    static long toAmount(GrantExperienceRequestDto request) {
        if (request.amount() == null) {
            throw new InvalidCharacterInputException("XP amount is required.");
        }

        try {
            long amount = request.amount().longValueExact();
            // Development transport limit, not the trusted server reward limit.
            if (amount < 1 || amount > 1_000_000_000L) {
                throw new InvalidCharacterInputException("Development XP amount must be between 1 and 1000000000.");
            }

            return amount;
        } catch (ArithmeticException exception) {
            throw new InvalidCharacterInputException("XP amount must be a representable integer.");
        }
    }

    static GrantExperienceResponseDto toResponse(ExperienceAwardResult result) {
        return new GrantExperienceResponseDto(result.previousLevel(), result.character().level(), result.levelsGained(),
                result.acceptedExperience(), result.trainingPointsAwarded(), result.character().experience(),
                result.character().unspentTrainingPoints());
    }
}
