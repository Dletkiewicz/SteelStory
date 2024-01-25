package pl.steelstory.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import pl.steelstory.model.item.dto.BackpackItemDto;

@Entity
@Table(name = "backpack_items")
public class BackpackItemEntity extends ItemEntity {

  @ManyToOne
  @JoinColumn(name = "backpack_id")
  private BackpackEntity backpack;

  @ManyToOne
  @JoinColumn(name = "item_id")
  private ItemEntity item;

  public BackpackItemDto toModel() {
    return new BackpackItemDto(businessId, name, type, itemRarity, requiredLevel, weight);
  }

  public static BackpackItemEntity create(BackpackEntity backpack, ItemEntity item) {
    var entity = new BackpackItemEntity();
    entity.backpack = backpack;
    entity.item = item;
    return entity;
  }

}
