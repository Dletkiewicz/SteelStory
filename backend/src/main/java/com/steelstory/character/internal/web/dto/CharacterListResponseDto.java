package com.steelstory.character.internal.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record CharacterListResponseDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<CharacterResponseDto> characters
) {
    public CharacterListResponseDto {
        characters = List.copyOf(characters);
    }
}
