package pl.steelstory.exception.character;

import java.util.UUID;

public class CharacterNotFoundException extends RuntimeException {

  public CharacterNotFoundException(UUID id) {
    super("Character [" + id + "] not found");
  }

  public CharacterNotFoundException(String name) {
    super("Character [" + name + "] not found");
  }

}
