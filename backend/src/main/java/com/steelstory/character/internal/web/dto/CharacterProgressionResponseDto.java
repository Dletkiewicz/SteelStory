package com.steelstory.character.internal.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CharacterProgressionResponseDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long currentLevelExperience,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0",
                description = "Incremental requirement, or zero at maximum level.") long nextLevelExperience,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long remainingExperience,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean maximumLevel
) {
}
