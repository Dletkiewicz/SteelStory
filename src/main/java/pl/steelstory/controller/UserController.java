package pl.steelstory.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.steelstory.model.CreateUserDto;
import pl.steelstory.model.UserDto;
import pl.steelstory.service.UserService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

  private final UserService users;

  @GetMapping("/users/{userId}")
  public UserDto get(@PathVariable UUID userId) {
    return users.get(userId);
  }

  @PostMapping("/users")
  public ResponseEntity<UserDto> save(@RequestBody CreateUserDto payload) {
    var user = users.save(payload);
    return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{userId}").build(user.businessId())).body(user);
  }

  @DeleteMapping("/users/{userId}")
  public ResponseEntity<Void> delete(@PathVariable UUID userId) {
    users.delete(userId);
    return ResponseEntity.noContent().build();
  }
}
