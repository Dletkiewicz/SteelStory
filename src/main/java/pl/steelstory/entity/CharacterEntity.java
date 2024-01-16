package pl.steelstory.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.NaturalId;
import pl.steelstory.model.character.CharacterClassType;
import pl.steelstory.model.character.dto.CharacterDto;
import pl.steelstory.model.character.dto.CreateCharacterDto;
import pl.steelstory.model.character.dto.UpdateCharacterDto;

import java.util.UUID;

@Entity
@Table(name = "characters")
public class CharacterEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id")
  private UUID databaseId;

  @NaturalId
  @Column(name = "business_id")
  private UUID businessId;

  @JoinColumn(name = "user_id")
  @OneToOne(fetch = FetchType.LAZY)
  private UserEntity user;

  @Column(name = "name")
  private String name;

  @Column(name = "level")
  private int level;

  @Column(name = "experience")
  private long experience;

  @Column(name = "class")
  @Enumerated(EnumType.STRING)
  private CharacterClassType characterClass;

  @Column(name = "strength")
  private int strength;

  @Column(name = "dexterity")
  private int dexterity;

  @Column(name = "intelligence")
  private int intelligence;

  @Column(name = "stamina")
  private int stamina;

  public CharacterDto toModel() {
    return new CharacterDto(businessId, name, level, experience, characterClass, strength, dexterity, intelligence, stamina);
  }

  public static CharacterEntity create(CreateCharacterDto character) {
    var entity = new CharacterEntity();
    entity.businessId = UUID.randomUUID();
    entity.name = character.name();
    entity.level = 0;
    entity.experience = 0;
    entity.characterClass = character.characterClass();
    entity.setAttributes(entity);
    entity.stamina = 100;
    return entity;
  }

  public void update(UpdateCharacterDto character) {
    name = character.name();
  }

  private void setAttributes(CharacterEntity entity) {

    switch (entity.characterClass) {

      case MAGE -> {
        entity.intelligence = 12;
        entity.dexterity = 3;
        entity.strength = 3;
      }
      case ARCHER -> {
        entity.intelligence = 3;
        entity.dexterity = 12;
        entity.strength = 3;
      }
      case BERSERK -> {
        entity.intelligence = 3;
        entity.dexterity = 3;
        entity.strength = 12;
      }
      case PALADIN -> {
        entity.intelligence = 7;
        entity.strength = 7;
        entity.dexterity = 3;
      }
    }
  }
}
