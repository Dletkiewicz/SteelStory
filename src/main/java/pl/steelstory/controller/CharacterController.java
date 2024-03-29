package pl.steelstory.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.steelstory.exception.character.CharacterNameTakenException;
import pl.steelstory.exception.character.CharacterNotFoundException;
import pl.steelstory.model.character.dto.CharacterDto;
import pl.steelstory.model.character.dto.CreateCharacterDto;
import pl.steelstory.model.character.dto.UpdateCharacterDto;
import pl.steelstory.service.CharacterService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/characters")
@RequiredArgsConstructor
public class CharacterController {

  private final CharacterService characters;

  @GetMapping("/{characterId}")
  public CharacterDto getCharacter(@PathVariable UUID characterId) {
    try {
      return characters.get(characterId);
    } catch (CharacterNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getLocalizedMessage(), e);
    }
  }

  @PostMapping()
  public ResponseEntity<CharacterDto> saveCharacter(@RequestParam UUID userId, @RequestBody CreateCharacterDto payload) {
    try {
      var character = characters.save(userId, payload);
      return ResponseEntity.created(
              ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{characterId}").build(character.businessId()))
          .body(character);
    } catch (CharacterNameTakenException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getLocalizedMessage(), e);
    }
  }

  @PutMapping("/{characterId}")
  public ResponseEntity<CharacterDto> updateCharacter(@PathVariable UUID characterId,
      @RequestBody UpdateCharacterDto payload) {
    try {
      var character = characters.update(characterId, payload);
      return ResponseEntity.ok(character);
    } catch (CharacterNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getLocalizedMessage(), e);
    }
  }

  @DeleteMapping("/{characterId}")
  public ResponseEntity<Void> deleteCharacter(@PathVariable UUID characterId) {
    try {
      characters.delete(characterId);
      return ResponseEntity.noContent().build();
    } catch (CharacterNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getLocalizedMessage(), e);
    }
  }
}
