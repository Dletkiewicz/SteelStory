package com.steelstory.character.internal.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record GrantExperienceRequestDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "integer", format = "int64",
                minimum = "1", maximum = "1000000000") BigDecimal amount
) {
}
