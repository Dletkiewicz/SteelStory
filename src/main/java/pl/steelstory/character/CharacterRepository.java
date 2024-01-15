package pl.steelstory.character;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CharacterRepository extends JpaRepository<CharacterEntity, UUID> {

  boolean existsByName(String name);

  boolean existsByBusinessId(UUID id);

  Optional<CharacterEntity> findByBusinessId(UUID id);

  void deleteByBusinessId(UUID id);
}
