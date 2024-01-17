package pl.steelstory.entity;

import jakarta.persistence.*;
import pl.steelstory.model.item.WeaponType;

@Entity
@Table(name = "weapons")
public class WeaponEntity extends ItemEntity {

  @Enumerated(EnumType.STRING)
  @Column(name = "type")
  private WeaponType type;

  @Column(name = "attack")
  private int attack;

}
