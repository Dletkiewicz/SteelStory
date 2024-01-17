package pl.steelstory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "weapons")
public class WeaponEntity extends ItemEntity {

  @Column(name = "attack")
  private int attack;

}
