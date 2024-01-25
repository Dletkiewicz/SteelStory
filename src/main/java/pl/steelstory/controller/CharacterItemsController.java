package pl.steelstory.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.steelstory.exception.character.CharacterNotFoundException;
import pl.steelstory.exception.item.ItemNotFoundException;
import pl.steelstory.model.character.dto.BackpackDto;
import pl.steelstory.model.item.dto.BackpackItemDto;
import pl.steelstory.service.CharacterItemService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/character")
@RequiredArgsConstructor
public class CharacterItemsController {

  private final CharacterItemService items;

  @GetMapping("/{characterId}/backpack")
  public BackpackDto getBackpackItems(@PathVariable UUID characterId) {
    try {
      return items.getBackpack(characterId);
    } catch (CharacterNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getLocalizedMessage(), e);
    }
  }

  @PostMapping("/{characterId}/backpack")
  public ResponseEntity<BackpackItemDto> saveItem(@PathVariable UUID characterId, @RequestParam UUID itemId) {
    try {
      var item = items.save(characterId, itemId);
      return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{itemId}").build(item.id())).body(item);
    } catch (CharacterNotFoundException | ItemNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getLocalizedMessage(), e);
    }
  }

  @DeleteMapping("/{characterId}/backpack/{itemId}")
  public ResponseEntity<Void> deleteItem(@PathVariable UUID characterId, @PathVariable UUID itemId) {
    try {
      items.deleteBackpackItem(characterId, itemId);
      return ResponseEntity.noContent().build();
    } catch (CharacterNotFoundException | ItemNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getLocalizedMessage(), e);
    }
  }
}
