package pl.steelstory.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.steelstory.user.exception.UserNotFoundException;
import pl.steelstory.user.exception.UsernameTakenException;
import pl.steelstory.user.model.CreateUserDto;
import pl.steelstory.user.model.dto.UserDto;
import pl.steelstory.user.service.UserService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

  private final UserService users;

  @GetMapping("/users/{userId}")
  public UserDto get(@PathVariable UUID userId) {
    try {
      return users.get(userId);
    } catch (UserNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getLocalizedMessage(), e);
    }
  }

  @PostMapping("/users")
  public ResponseEntity<UserDto> save(@RequestBody CreateUserDto payload) {
    try {
      var user = users.save(payload);
      return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{userId}").build(user.businessId())).body(user);
    } catch (UsernameTakenException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getLocalizedMessage(), e);
    }
  }

  @DeleteMapping("/users/{userId}")
  public ResponseEntity<Void> delete(@PathVariable UUID userId) {
    try {
      users.delete(userId);
      return ResponseEntity.noContent().build();
    } catch (UserNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getLocalizedMessage(), e);
    }
  }
}
