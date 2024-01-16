package pl.steelstory.exception.user;

public class UsernameTakenException extends RuntimeException {

  public UsernameTakenException(String username) {
    super("Username + [" + username + "] is already taken");
  }
}
