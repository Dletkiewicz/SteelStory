package com.steelstory.bootstrap.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "StatusResponse")
public record StatusResponseDto(
        @Schema(
                allowableValues = "steelstory",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String application,
        @Schema(
                allowableValues = "UP",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String status
) {
}
