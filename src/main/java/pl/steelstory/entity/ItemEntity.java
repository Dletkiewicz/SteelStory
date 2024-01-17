package pl.steelstory.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.NaturalId;
import pl.steelstory.model.item.ItemRarity;
import pl.steelstory.model.item.ItemType;

import java.util.UUID;

@Entity
@Table(name = "items")
@Inheritance(strategy = InheritanceType.JOINED)
public class ItemEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id")
  private UUID databaseId;

  @NaturalId
  @Column(name = "business_id")
  private UUID businessId;

  @Column(name = "name")
  private String name;

  @Column(name = "required_level")
  private int requiredLevel;

  @Enumerated(EnumType.STRING)
  @Column(name = "rarity")
  private ItemRarity itemRarity;

  @Enumerated(EnumType.STRING)
  @Column(name = "type")
  private ItemType type;

}
