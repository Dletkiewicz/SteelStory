package pl.steelstory.entity;

import jakarta.persistence.*;
import lombok.Getter;
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
  protected UUID databaseId;

  @Getter
  @NaturalId
  @Column(name = "business_id")
  protected UUID businessId;

  @Column(name = "name")
  protected String name;

  @Column(name = "required_level")
  protected int requiredLevel;

  @Enumerated(EnumType.STRING)
  @Column(name = "rarity")
  protected ItemRarity itemRarity;

  @Enumerated(EnumType.STRING)
  @Column(name = "type")
  protected ItemType type;

  @Column(name = "weight")
  protected int weight;

}
