package pl.steelstory.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.NaturalId;
import pl.steelstory.model.item.ItemType;

import java.util.UUID;

@Entity
@Table(name = "equipped_items")
public class EquippedItemEntity extends ItemEntity {

  @ManyToOne
  @JoinColumn(name = "character_id")
  private CharacterEntity character;

  @ManyToOne
  @JoinColumn(name = "item_id")
  private ItemEntity item;

//  EquippedItemDto toModel() {
//    return new EquippedItemDto();
//  }
}
