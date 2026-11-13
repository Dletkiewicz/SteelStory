package com.steelstory.identity.internal.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoginRequest")
public record LoginRequestDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 320,
                description = "ASCII email; surrounding whitespace stripped, canonical lowercase; "
                        + "max 254 after normalization.")
        String email,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 128,
                accessMode = Schema.AccessMode.WRITE_ONLY,
                description = "1–128 Unicode code points, preserved exactly; no trimming or normalization. "
                        + "The minimum length for new passwords does not apply to login.")
        String password
) {
    @Override
    public String toString() {
        return "LoginRequestDto[REDACTED]";
    }
}
