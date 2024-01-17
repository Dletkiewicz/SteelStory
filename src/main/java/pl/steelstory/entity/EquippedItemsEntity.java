package pl.steelstory.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.NaturalId;

import java.util.UUID;

@Entity
@Table(name = "equipped_items")
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
  @JoinColumn(name = "armor_id")
  private ArmorEntity armor;

  @ManyToOne
  @JoinColumn(name = "weapon_id")
  private WeaponEntity weapon;

}
