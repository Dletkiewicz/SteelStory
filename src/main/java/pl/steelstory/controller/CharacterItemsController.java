package pl.steelstory.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.steelstory.model.character.dto.BackpackDto;
import pl.steelstory.service.BackpackItemService;
import pl.steelstory.service.CharacterService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/character")
@RequiredArgsConstructor
public class CharacterItemsController {

  private final BackpackItemService backpackItems;
  private final CharacterService characters;

  @GetMapping("/{characterId}/backpack")
  public BackpackDto getBackpackItems(@PathVariable UUID characterId) {
    return characters.getBackpack(characterId);
  }
}
