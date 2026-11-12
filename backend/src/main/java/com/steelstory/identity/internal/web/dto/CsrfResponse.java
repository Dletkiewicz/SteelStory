package com.steelstory.identity.internal.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CsrfResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = "X-CSRF-TOKEN") String headerName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String token
) {
    @Override
    public String toString() {
        return "CsrfResponse[REDACTED]";
    }
}
