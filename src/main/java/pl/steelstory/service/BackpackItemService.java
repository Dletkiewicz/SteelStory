package pl.steelstory.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.steelstory.entity.BackpackItemEntity;
import pl.steelstory.exception.character.BackpackNotFound;
import pl.steelstory.exception.character.CharacterNotFoundException;
import pl.steelstory.exception.item.BackpackItemNotFoundException;
import pl.steelstory.exception.item.ItemNotFoundException;
import pl.steelstory.model.character.dto.BackpackDto;
import pl.steelstory.model.item.dto.BackpackItemDto;
import pl.steelstory.model.item.dto.CreateBackpackItemDto;
import pl.steelstory.model.item.dto.DeleteBackpackItemDto;
import pl.steelstory.model.item.dto.ListBackpackItemsDto;
import pl.steelstory.repository.BackpackItemRepository;
import pl.steelstory.repository.BackpackRepository;
import pl.steelstory.repository.CharacterRepository;
import pl.steelstory.repository.ItemRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BackpackItemService {

  private final BackpackItemRepository backpackItems;
  private final BackpackRepository backpacks;
  private final CharacterRepository characters;
  private final ItemRepository items;

  public List<BackpackItemDto> list(UUID id) {
    var character = characters.findByBusinessId(id).orElseThrow(() -> new CharacterNotFoundException(id));

    return backpackItems.findAllByBackpackBusinessId(character.getBusinessId()).stream().map(BackpackItemEntity::toModel)
        .toList();
  }

  public BackpackDto save(CreateBackpackItemDto dto) {
    var backpack = backpacks.findByBusinessId(dto.backpackId()).orElseThrow(() -> new BackpackNotFound(dto.backpackId()));
    var item = items.findByBusinessId(dto.itemId()).orElseThrow(() -> new ItemNotFoundException(dto.itemId()));
    backpack.addItem(backpack, item);
    backpacks.save(backpack);
    return backpack.toModel();
  }

  public void delete(DeleteBackpackItemDto dto) {
    var character = characters.findByBusinessId(dto.characterId()).orElseThrow(() -> new CharacterNotFoundException(dto.characterId()));
    var item = backpackItems.findByBusinessIdAndBackpackBusinessId(dto.id(), character.getBusinessId()).orElseThrow(() -> new BackpackItemNotFoundException(dto.id()));

    backpackItems.deleteByBusinessId(item.getBusinessId());
  }
}
