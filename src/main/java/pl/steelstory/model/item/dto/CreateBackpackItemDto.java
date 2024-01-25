package pl.steelstory.model.item.dto;

import java.util.UUID;

public record CreateBackpackItemDto(UUID itemId, UUID backpackId) {
}
