package pl.steelstory.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.NaturalId;
import pl.steelstory.model.item.ItemRarity;
import pl.steelstory.model.item.WeaponType;

import java.util.UUID;

@Entity
@Table(name = "weapons")
public class WeaponEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id")
  private UUID databaseId;

  @NaturalId
  @Column(name = "business_id")
  private UUID businessId;

  @Column(name = "name")
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "item_type")
  private WeaponType type;

  @Column(name = "attack")
  private int attack;

  @Column(name = "required_level")
  private int requiredLevel;

  @Enumerated(EnumType.STRING)
  @Column(name = "rarity")
  private ItemRarity itemRarity;
}
