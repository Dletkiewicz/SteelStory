package pl.steelstory.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.steelstory.entity.CharacterEntity;
import pl.steelstory.exception.character.CharacterNameTakenException;
import pl.steelstory.exception.character.CharacterNotFoundException;
import pl.steelstory.model.character.dto.CharacterDto;
import pl.steelstory.model.character.dto.CreateCharacterDto;
import pl.steelstory.model.character.dto.UpdateCharacterDto;
import pl.steelstory.repository.CharacterRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CharacterService {

  private final CharacterRepository characters;

  public CharacterDto save(CreateCharacterDto character) {
    if (characters.existsByName(character.name())) {
      throw new CharacterNameTakenException(character.name());
    }

    var entity = CharacterEntity.create(character);
    characters.save(entity);
    return entity.toModel();
  }

  public CharacterDto get(UUID id) {
    return characters.findByBusinessId(id)
        .map(CharacterEntity::toModel)
        .orElseThrow(() -> new CharacterNotFoundException(id));
  }

  public void delete(UUID id) {
    if (!characters.existsByBusinessId(id)) {
      throw new CharacterNotFoundException(id);
    }

    characters.deleteByBusinessId(id);
  }

  public CharacterDto update(UUID id, UpdateCharacterDto character) {
    var entity = characters.findByBusinessId(id).orElseThrow(() -> new CharacterNotFoundException(id));

    entity.update(character);
    return entity.toModel();
  }
}
