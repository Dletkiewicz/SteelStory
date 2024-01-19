package pl.steelstory.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import pl.steelstory.model.item.dto.EquippedItemDto;

@Entity
@Table(name = "equipped_items")
public class EquippedItemEntity extends ItemEntity {

  @ManyToOne
  @JoinColumn(name = "character_id")
  private CharacterEntity character;

  @ManyToOne
  @JoinColumn(name = "item_id")
  private ItemEntity item;

  EquippedItemDto toModel() {
    return new EquippedItemDto(businessId, name, type, itemRarity, requiredLevel, weight);
  }
}
