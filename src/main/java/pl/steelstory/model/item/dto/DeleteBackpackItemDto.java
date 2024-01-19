package pl.steelstory.model.item.dto;

import java.util.UUID;

public record DeleteBackpackItemDto(UUID id, UUID characterId) {
}
