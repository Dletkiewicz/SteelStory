package pl.steelstory.exception.item;

import java.util.UUID;

public class BackpackItemNotFoundException extends RuntimeException {

  public BackpackItemNotFoundException(UUID id) {
    super("Item [" + id + "] not found in backpack");
  }
}
