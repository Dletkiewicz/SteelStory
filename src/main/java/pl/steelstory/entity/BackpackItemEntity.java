package pl.steelstory.entity;

import jakarta.persistence.*;
import pl.steelstory.model.item.dto.BackpackItemDto;

@Entity
@Table(name = "backpack_items")
public class BackpackItemEntity extends ItemEntity {

  @ManyToOne
  @JoinColumn(name = "character_id")
  private CharacterEntity character;

  @ManyToOne
  @JoinColumn(name = "item_id")
  private ItemEntity item;

  @Column(name = "capacity")
  private int capacity;

  public BackpackItemDto toModel() {
    return new BackpackItemDto(businessId, name, type, itemRarity, requiredLevel, weight);
  }

}
