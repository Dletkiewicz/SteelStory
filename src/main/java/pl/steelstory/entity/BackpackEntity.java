package pl.steelstory.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.NaturalId;

import java.util.UUID;

@Entity
@Table(name = "backpack_items")
public class BackpackEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id")
  private UUID databaseId;

  @NaturalId
  @Column(name = "business_id")
  private UUID businessId;

  @OneToOne
  @JoinColumn(name = "character_id")
  private CharacterEntity character;

  @ManyToOne
  @JoinColumn(name = "item_id")
  private ItemEntity item;

  @Column(name = "capacity")
  private int capacity;

}
