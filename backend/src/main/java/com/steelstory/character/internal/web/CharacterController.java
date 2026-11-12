package com.steelstory.character.internal.web;

import com.steelstory.character.internal.application.CharacterService;
import com.steelstory.character.internal.application.CreateCharacterCommand;
import com.steelstory.character.internal.domain.CharacterId;
import com.steelstory.character.internal.domain.PlayerCharacter;
import com.steelstory.character.internal.web.dto.CharacterListResponseDto;
import com.steelstory.character.internal.web.dto.CharacterProblemResponseDto;
import com.steelstory.character.internal.web.dto.CharacterResponseDto;
import com.steelstory.character.internal.web.dto.CreateCharacterRequestDto;
import com.steelstory.identity.api.AccountId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/characters", produces = "application/json")
@SecurityRequirement(name = "sessionCookie")
@ApiResponse(responseCode = "401", description = "Authentication required; account must be active",
        content = @Content(mediaType = "application/problem+json",
                schema = @Schema(implementation = CharacterProblemResponseDto.class)))
public class CharacterController {
    private final CharacterService characterService;

    public CharacterController(CharacterService characterService) {
        this.characterService = characterService;
    }

    @Operation(operationId = "createCharacter", summary = "Create an owned character",
            description = "Distribute exactly 10 points above base 5. At most 3 characters per account.")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true,
            description = "Token from getCsrfToken")
    @ApiResponse(responseCode = "201", description = "Character created")
    @ApiResponse(responseCode = "400", description = "Invalid name, class or starting allocation",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = CharacterProblemResponseDto.class)))
    @ApiResponse(responseCode = "403", description = "Missing or invalid CSRF token",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = CharacterProblemResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Name taken or character limit reached",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = CharacterProblemResponseDto.class)))
    @PostMapping(consumes = "application/json")
    public ResponseEntity<CharacterResponseDto> create(
            @RequestBody CreateCharacterRequestDto request, Authentication principal) {
        CreateCharacterCommand command = CharacterDtoMapper.toCommand(request);
        PlayerCharacter character = characterService.create(accountId(principal), command);

        return ResponseEntity.created(URI.create("/api/v1/characters/" + character.id().value()))
                .body(CharacterDtoMapper.toResponse(character));
    }

    @Operation(operationId = "listOwnCharacters", summary = "List owned characters in creation order")
    @ApiResponse(responseCode = "200", description = "Owned characters ordered by creation time, then ID")
    @GetMapping
    public ResponseEntity<CharacterListResponseDto> list(Authentication principal) {
        List<CharacterResponseDto> characters = characterService.listOwn(accountId(principal)).stream()
                .map(CharacterDtoMapper::toResponse).toList();
        return ResponseEntity.ok(new CharacterListResponseDto(characters));
    }

    @Operation(operationId = "getOwnCharacter", summary = "Get an owned character")
    @ApiResponse(responseCode = "200", description = "Owned character")
    @ApiResponse(responseCode = "400", description = "Invalid character ID",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = CharacterProblemResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Unknown or unowned character",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = CharacterProblemResponseDto.class)))
    @GetMapping("/{id}")
    public ResponseEntity<CharacterResponseDto> get(@PathVariable UUID id, Authentication principal) {
        PlayerCharacter character = characterService.getOwn(accountId(principal), new CharacterId(id));
        return ResponseEntity.ok(CharacterDtoMapper.toResponse(character));
    }

    private AccountId accountId(Authentication principal) {
        return new AccountId(UUID.fromString(principal.getName()));
    }
}
