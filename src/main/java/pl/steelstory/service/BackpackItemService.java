package pl.steelstory.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.steelstory.entity.BackpackItemEntity;
import pl.steelstory.model.item.BackpackItemDto;
import pl.steelstory.model.item.ListBackpackItemsDto;
import pl.steelstory.repository.BackpackItemRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BackpackItemService {

  private final BackpackItemRepository items;

  public List<BackpackItemDto> list(ListBackpackItemsDto dto) {

    return items.findAllByCharacterBusinessId(dto.characterId()).stream()
        .map(BackpackItemEntity::toModel)
        .toList();
  }

}
