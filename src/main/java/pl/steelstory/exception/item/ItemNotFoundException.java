package pl.steelstory.exception.item;

import java.util.UUID;

public class ItemNotFoundException extends RuntimeException {

  public ItemNotFoundException(UUID id) {
    super("Item [" + id + "] not found");
  }
}
