package pl.steelstory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "armors")
public class ArmorEntity extends ItemEntity {

  @Column(name = "defense")
  private int defense;

}
