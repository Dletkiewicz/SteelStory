package pl.steelstory.character;

import jakarta.persistence.*;
import org.hibernate.annotations.NaturalId;
import pl.steelstory.character.model.CharacterClassType;
import pl.steelstory.character.model.dto.CharacterDto;
import pl.steelstory.character.model.dto.CreateCharacterDto;
import pl.steelstory.user.entity.UserEntity;

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

  CharacterDto toModel() {
    return new CharacterDto(businessId, name, level, experience, characterClass, strength, intelligence, stamina);
  }

  static CharacterEntity create(CreateCharacterDto character) {
    var entity = new CharacterEntity();
    entity.businessId = UUID.randomUUID();
    entity.name = character.name();
    entity.level = 0;
    entity.experience = 0;
    entity.characterClass = character.characterClass();
    entity.strength = 5;
    entity.dexterity = 5;
    entity.intelligence = 5;
    entity.stamina = 100;
    return entity;
  }
}
