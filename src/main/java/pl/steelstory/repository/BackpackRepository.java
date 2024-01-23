package pl.steelstory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.steelstory.entity.BackpackEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BackpackRepository extends JpaRepository<BackpackEntity, UUID> {

  Optional<BackpackEntity> findByCharacterBusinessId(UUID id);

  Optional<BackpackEntity> findByBusinessId(UUID id);
}
