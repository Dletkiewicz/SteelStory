package pl.steelstory.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.NaturalId;
import pl.steelstory.model.item.ItemType;

import java.util.UUID;

@Entity
@Table(name = "equipped_item")
public class EquippedItemsEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id")
  private UUID databaseId;

  @NaturalId
  @Column(name = "business_id")
  private UUID businessId;

  @ManyToOne
  @JoinColumn(name = "character_id")
  private CharacterEntity character;

  @ManyToOne
  @JoinColumn(name = "item_id")
  private ItemEntity item;

  @Enumerated(EnumType.STRING)
  @Column(name = "item_type")
  private ItemType type;

}
