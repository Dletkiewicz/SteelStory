package com.steelstory.character.internal.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreateCharacterRequestDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 3, maxLength = 20,
                pattern = "^[A-Za-z0-9]+(?: [A-Za-z0-9]+)*$") String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"WARRIOR", "ARCHER", "MAGE"})
        String characterClass,
        @Schema(allowableValues = {"MALE", "FEMALE"},
                description = "Cosmetic appearance. Omitted or null uses FEMALE for ARCHER, MALE otherwise.")
        String appearanceGender,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Nonnegative integer points summing to 10.")
        StartingAllocationRequestDto startingAllocation
) {
}
