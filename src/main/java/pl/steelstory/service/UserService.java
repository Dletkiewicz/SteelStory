package pl.steelstory.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.steelstory.entity.UserEntity;
import pl.steelstory.exception.UserNotFoundException;
import pl.steelstory.exception.UsernameTakenException;
import pl.steelstory.model.UserDto;
import pl.steelstory.repository.UserRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository users;

  public UserDto save(UserDto user) {
    if (users.existsByUsername(user.username().toLowerCase())) {
      throw new UsernameTakenException(user.username());
    }
    var entity = new UserEntity(UUID.randomUUID(), UUID.randomUUID(), user.username(), user.password());
    users.save(entity);

    return entity.toModel();
  }

  public void delete(UUID id) {
    if (!users.existsByBusinessId(id)) {
      throw new UserNotFoundException(id);
    }

    users.deleteByBusinessId(id);
  }

  public UserDto get(UUID id) {
    return users.findByBusinessId(id)
        .map(UserEntity::toModel)
        .orElseThrow(() -> new UserNotFoundException(id));
  }

}
