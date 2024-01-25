package pl.steelstory.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.steelstory.entity.BackpackEntity;
import pl.steelstory.entity.CharacterEntity;
import pl.steelstory.exception.character.CharacterNameTakenException;
import pl.steelstory.exception.character.CharacterNotFoundException;
import pl.steelstory.exception.user.UserNotFoundException;
import pl.steelstory.model.character.dto.CharacterDto;
import pl.steelstory.model.character.dto.CreateCharacterDto;
import pl.steelstory.model.character.dto.UpdateCharacterDto;
import pl.steelstory.repository.BackpackRepository;
import pl.steelstory.repository.CharacterRepository;
import pl.steelstory.repository.UserRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CharacterService {

  private final CharacterRepository characters;
  private final BackpackRepository backpacks;
  private final UserRepository users;

  @Transactional
  public CharacterDto save(UUID userId, CreateCharacterDto character) {
    if (characters.existsByName(character.name())) {
      throw new CharacterNameTakenException(character.name());
    }

    var user = users.findByBusinessId(userId).orElseThrow(() -> new UserNotFoundException(userId));
    var entity = CharacterEntity.create(user, character);
    var backpack = BackpackEntity.create(entity);
    characters.save(entity);
    backpacks.save(backpack);
    return entity.toModel();
  }

  public CharacterDto get(UUID id) {
    return characters.findByBusinessId(id)
        .map(CharacterEntity::toModel)
        .orElseThrow(() -> new CharacterNotFoundException(id));
  }

  @Transactional
  public void delete(UUID id) {
    if (!characters.existsByBusinessId(id)) {
      throw new CharacterNotFoundException(id);
    }

    characters.deleteByBusinessId(id);
  }

  @Transactional
  public CharacterDto update(UUID id, UpdateCharacterDto character) {
    var entity = characters.findByBusinessId(id).orElseThrow(() -> new CharacterNotFoundException(id));

    entity.update(character);
    return entity.toModel();
  }
}
