package com.steelstory.character.internal.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CharacterAttributesResponseDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int strength,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int dexterity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int intelligence,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int constitution,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int luck
) {
}
