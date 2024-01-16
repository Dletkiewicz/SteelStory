package pl.steelstory.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.steelstory.entity.UserEntity;
import pl.steelstory.exception.user.UserNotFoundException;
import pl.steelstory.exception.user.UsernameTakenException;
import pl.steelstory.model.user.CreateUserDto;
import pl.steelstory.model.user.UserDto;
import pl.steelstory.repository.UserRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository users;

  @Transactional
  public UserDto save(CreateUserDto user) {
    if (users.existsByUsername(user.username().toLowerCase())) {
      throw new UsernameTakenException(user.username());
    }
    var entity = UserEntity.create(user);
    users.save(entity);

    return entity.toModel();
  }

  @Transactional
  public void delete(UUID id) {
    if (!users.existsByBusinessId(id)) {
      throw new UserNotFoundException(id);
    }

    users.deleteByBusinessId(id);
  }

  public UserDto get(UUID id) {
    return users.findByBusinessId(id).map(UserEntity::toModel).orElseThrow(() -> new UserNotFoundException(id));
  }

}
