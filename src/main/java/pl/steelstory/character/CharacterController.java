package pl.steelstory.character;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.steelstory.character.exception.CharacterNameTakenException;
import pl.steelstory.character.exception.CharacterNotFoundException;
import pl.steelstory.character.model.dto.CharacterDto;
import pl.steelstory.character.model.dto.CreateCharacterDto;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CharacterController {

  private final CharacterService characters;

  @GetMapping("/characters/{characterId}")
  public CharacterDto getCharacter(@PathVariable UUID characterId) {
    try {
      return characters.get(characterId);
    } catch (CharacterNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getLocalizedMessage(), e);
    }
  }

  @PostMapping("/characters")
  public ResponseEntity<CharacterDto> saveCharacter(@RequestBody CreateCharacterDto payload) {
    try {
      var character = characters.save(payload);
      return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{characterId}").build(character.businessId())).body(character);
    } catch (CharacterNameTakenException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getLocalizedMessage(), e);
    }
  }

  @DeleteMapping("/characters/{characterId}")
  public ResponseEntity<Void> deleteCharacter(@PathVariable UUID characterId) {
    try {
      characters.delete(characterId);
      return ResponseEntity.noContent().build();
    } catch (CharacterNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getLocalizedMessage(), e);
    }
  }
}
