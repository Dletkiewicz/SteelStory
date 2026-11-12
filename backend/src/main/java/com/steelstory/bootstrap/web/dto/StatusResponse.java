package com.steelstory.bootstrap.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record StatusResponse(
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
