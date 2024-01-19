package pl.steelstory.model.item.dto;

import pl.steelstory.model.item.ItemRarity;
import pl.steelstory.model.item.ItemType;

import java.util.UUID;

public record BackpackItemDto(UUID id, String name, ItemType type, ItemRarity rarity, int requiredLevel, int weight) {
}
