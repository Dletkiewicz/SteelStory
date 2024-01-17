package pl.steelstory.entity;

import jakarta.persistence.*;
import pl.steelstory.model.item.ArmorType;

@Entity
@Table(name = "armors")
public class ArmorEntity extends ItemEntity {

  @Enumerated(EnumType.STRING)
  @Column(name = "type")
  private ArmorType type;

  @Column(name = "defense")
  private int defense;

}
