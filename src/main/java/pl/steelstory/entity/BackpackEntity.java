package pl.steelstory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.NaturalId;
import pl.steelstory.model.character.dto.BackpackDto;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "backpacks")
public class BackpackEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id")
  private UUID databaseId;

  @Getter
  @NaturalId
  @Column(name = "business_id")
  private UUID businessId;

  @OneToOne
  @JoinColumn(name = "character_id")
  private CharacterEntity character;

  @OneToMany(mappedBy = "backpack")
  private List<BackpackItemEntity> items;

  @Column(name = "capacity")
  private int capacity;

  public BackpackDto toModel() {
    return new BackpackDto(businessId,
        character.getBusinessId(),
        items != null ? items.stream().map(BackpackItemEntity::toModel).collect(Collectors.toList()) : Collections.emptyList(),
        capacity);
  }

  public static BackpackEntity create(CharacterEntity character) {
    var entity = new BackpackEntity();
    entity.businessId = UUID.randomUUID();
    entity.character = character;
    entity.items = null;
    entity.capacity = 100;
    return entity;
  }
}
