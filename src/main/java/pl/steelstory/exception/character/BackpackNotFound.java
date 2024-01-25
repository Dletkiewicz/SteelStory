package pl.steelstory.exception.character;

import java.util.UUID;

public class BackpackNotFound extends RuntimeException {

  public BackpackNotFound(UUID id) {
    super("Backpack [" + id + "] not found");
  }
}
