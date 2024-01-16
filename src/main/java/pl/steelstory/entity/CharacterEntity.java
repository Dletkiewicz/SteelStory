package pl.steelstory.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.NaturalId;
import pl.steelstory.model.character.CharacterClassType;
import pl.steelstory.model.character.dto.UpdateCharacterDto;
import pl.steelstory.model.character.dto.CharacterDto;
import pl.steelstory.model.character.dto.CreateCharacterDto;

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
    entity.name = character.getName();
    entity.level = 0;
    entity.experience = 0;
    entity.characterClass = character.getCharacterClass();
    entity.strength = character.b;
    entity.dexterity = 5;
    entity.intelligence = 5;
    entity.stamina = 100;
    return entity;
  }

  public void update(UpdateCharacterDto character) {
    name = character.name();
  }
}
