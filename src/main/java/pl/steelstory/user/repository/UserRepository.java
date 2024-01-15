package pl.steelstory.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.steelstory.user.entity.UserEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

  boolean existsByUsername(String username);

  boolean existsByBusinessId(UUID id);

  void deleteByBusinessId(UUID id);

  Optional<UserEntity> findByBusinessId(UUID id);

}
