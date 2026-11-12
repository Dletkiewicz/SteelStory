package com.steelstory.character.internal.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record CharacterResponseDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"WARRIOR", "ARCHER", "MAGE"})
        String characterClass,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1") int level,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long experience,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int unspentTrainingPoints,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CharacterAttributesResponseDto startingAttributes,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CharacterAttributesResponseDto trainedAttributes,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant updatedAt
) {
}
