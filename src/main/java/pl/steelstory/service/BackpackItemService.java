package pl.steelstory.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.steelstory.entity.BackpackItemEntity;
import pl.steelstory.exception.character.CharacterNotFoundException;
import pl.steelstory.exception.item.BackpackItemNotFoundException;
import pl.steelstory.model.item.dto.BackpackItemDto;
import pl.steelstory.model.item.dto.DeleteBackpackItemDto;
import pl.steelstory.model.item.dto.ListBackpackItemsDto;
import pl.steelstory.repository.BackpackItemRepository;
import pl.steelstory.repository.CharacterRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BackpackItemService {

  private final BackpackItemRepository items;
  private final CharacterRepository characters;

  public List<BackpackItemDto> list(ListBackpackItemsDto dto) {
    var character = characters.findByBusinessId(dto.characterId()).orElseThrow(() -> new CharacterNotFoundException(dto.characterId()));

    return items.findAllByBackpackBusinessId(character.getBusinessId()).stream().map(BackpackItemEntity::toModel)
        .toList();
  }

  public void delete(DeleteBackpackItemDto dto) {
    var character = characters.findByBusinessId(dto.characterId()).orElseThrow(() -> new CharacterNotFoundException(dto.characterId()));
    var item = items.findByBusinessIdAndBackpackBusinessId(dto.id(), character.getBusinessId()).orElseThrow(() -> new BackpackItemNotFoundException(dto.id()));

    items.deleteByBusinessId(item.getBusinessId());
  }
}
