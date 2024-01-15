package pl.steelstory.character;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.steelstory.character.exception.CharacterNameTakenException;
import pl.steelstory.character.exception.CharacterNotFoundException;
import pl.steelstory.character.model.dto.CharacterDto;
import pl.steelstory.character.model.dto.CreateCharacterDto;

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
}
