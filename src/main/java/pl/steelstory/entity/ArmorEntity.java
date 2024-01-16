package pl.steelstory.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.NaturalId;
import pl.steelstory.model.item.ArmorType;
import pl.steelstory.model.item.ItemRarity;

import java.util.UUID;

@Entity
@Table(name = "armors")
public class ArmorEntity {

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
  @Column(name = "type")
  private ArmorType type;

  @Column(name = "defense")
  private int defense;

  @Column(name = "required_level")
  private int requiredLevel;

  @Enumerated(EnumType.STRING)
  @Column(name = "rarity")
  private ItemRarity itemRarity;
}
