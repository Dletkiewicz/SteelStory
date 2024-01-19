package pl.steelstory.model.item;

import java.util.UUID;

public record BackpackItemDto(UUID id, String name, ItemType type, ItemRarity rarity, int requiredLevel, int weight) {
}
