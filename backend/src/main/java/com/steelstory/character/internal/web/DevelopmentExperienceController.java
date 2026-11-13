package com.steelstory.character.internal.web;

import com.steelstory.character.internal.application.CharacterService;
import com.steelstory.character.internal.application.ExperienceAwardResult;
import com.steelstory.character.internal.domain.CharacterId;
import com.steelstory.character.internal.web.dto.GrantExperienceRequestDto;
import com.steelstory.character.internal.web.dto.GrantExperienceResponseDto;
import com.steelstory.identity.api.AccountId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("local")
@ConditionalOnProperty(name = "steelstory.dev.xp.enabled", havingValue = "true", matchIfMissing = false)
@RequestMapping(value = "/api/v1/dev/characters", produces = "application/json")
@SecurityRequirement(name = "sessionCookie")
public class DevelopmentExperienceController {
    private final CharacterService characterService;

    public DevelopmentExperienceController(CharacterService characterService) {
        this.characterService = characterService;
    }

    @PostMapping(value = "/{id}/grant-xp", consumes = "application/json")
    @Operation(operationId = "grantDevelopmentExperience", summary = "Local opt-in XP grant to an owned character",
            description = "Development only. Requires local profile, steelstory.dev.xp.enabled=true, session and CSRF. "
                    + "Each call is a new reward; never automatically retry it.")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true,
            description = "Token from getCsrfToken")
    public ResponseEntity<GrantExperienceResponseDto> grant(@PathVariable UUID id,
            @RequestBody GrantExperienceRequestDto request, Authentication principal) {
        long amount = DevelopmentExperienceDtoMapper.toAmount(request);
        AccountId accountId = new AccountId(UUID.fromString(principal.getName()));
        ExperienceAwardResult result = characterService.awardExperience(accountId, new CharacterId(id), amount);
        return ResponseEntity.ok(DevelopmentExperienceDtoMapper.toResponse(result));
    }
}
