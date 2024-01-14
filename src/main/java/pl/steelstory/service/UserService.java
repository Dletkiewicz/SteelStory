package pl.steelstory.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.steelstory.entity.UserEntity;
import pl.steelstory.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository users;

}
