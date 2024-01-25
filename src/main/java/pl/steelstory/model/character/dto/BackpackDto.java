package pl.steelstory.model.character.dto;

import pl.steelstory.model.item.dto.BackpackItemDto;

import java.util.List;
import java.util.UUID;

public record BackpackDto(UUID id, UUID characterId, List<BackpackItemDto> items, int capacity) {
}
