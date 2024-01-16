package pl.steelstory.exception.character;

public class CharacterNameTakenException extends RuntimeException {

  public CharacterNameTakenException(String name) {
    super("Character name [" + name + "] is already taken");
  }
}
