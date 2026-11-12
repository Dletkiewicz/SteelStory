package com.steelstory.identity.internal.web.dto;

import com.steelstory.identity.internal.domain.Account;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "email", maxLength = 254) String email,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"ACTIVE", "DISABLED"}) String status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant updatedAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(account.id().value(), account.email().value(), account.status().name(),
                account.createdAt(), account.updatedAt());
    }
}
