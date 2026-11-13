package com.steelstory.character.internal.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record StartingAllocationRequestDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = Integer.class, minimum = "0")
        BigDecimal strength,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = Integer.class, minimum = "0")
        BigDecimal dexterity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = Integer.class, minimum = "0")
        BigDecimal intelligence,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = Integer.class, minimum = "0")
        BigDecimal constitution,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = Integer.class, minimum = "0")
        BigDecimal luck
) {
}
