package com.steelstory.identity.internal.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;

/**
 * Documents the flattened RFC 9457 wire representation emitted by Spring's ProblemDetail.
 * Framework input errors may omit the application-specific code.
 */
@Schema(name = "IdentityProblemResponse")
public record IdentityProblemResponseDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) URI type,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String detail,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) URI instance,
        String code
) {
}
