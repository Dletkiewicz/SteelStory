package pl.steelstory.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.steelstory.entity.BackpackItemEntity;
import pl.steelstory.exception.character.CharacterNotFoundException;
import pl.steelstory.exception.item.BackpackItemNotFoundException;
import pl.steelstory.exception.item.ItemNotFoundException;
import pl.steelstory.model.character.dto.BackpackDto;
import pl.steelstory.model.item.dto.BackpackItemDto;
import pl.steelstory.repository.BackpackItemRepository;
import pl.steelstory.repository.BackpackRepository;
import pl.steelstory.repository.CharacterRepository;
import pl.steelstory.repository.ItemRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CharacterItemService {

  private final BackpackItemRepository backpackItems;
  private final BackpackRepository backpacks;
  private final CharacterRepository characters;
  private final ItemRepository items;

  public BackpackDto getBackpack(UUID characterId) {
    var character = characters.findByBusinessId(characterId).orElseThrow(() -> new CharacterNotFoundException(characterId));
    var backpack = backpacks.getByCharacterBusinessId(character.getBusinessId());

    return backpack.toModel();
  }

  @Transactional
  public BackpackItemDto save(UUID characterId, UUID itemId) {
    var character = characters.findByBusinessId(characterId).orElseThrow(() -> new CharacterNotFoundException(characterId));
    var backpack = backpacks.getByCharacterBusinessId(character.getBusinessId());
    var item = BackpackItemEntity.create(backpack, items.findByBusinessId(itemId).orElseThrow(() -> new ItemNotFoundException(itemId)));
    backpackItems.save(item);
    return item.toModel();
  }

  @Transactional
  public void deleteBackpackItem(UUID characterId, UUID itemId) {
    var character = characters.findByBusinessId(characterId).orElseThrow(() -> new CharacterNotFoundException(characterId));
    var item = backpackItems.findByBusinessIdAndBackpackBusinessId(itemId, character.getBusinessId()).orElseThrow(() -> new BackpackItemNotFoundException(itemId));

    backpackItems.deleteByBusinessId(item.getBusinessId());
  }
}
