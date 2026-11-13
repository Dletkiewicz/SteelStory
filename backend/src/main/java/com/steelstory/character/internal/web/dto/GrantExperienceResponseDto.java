package com.steelstory.character.internal.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record GrantExperienceResponseDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int previousLevel,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int currentLevel,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int levelsGained,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long acceptedExperience,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int trainingPointsAwarded,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalExperience,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int unspentTrainingPoints
) {
}
