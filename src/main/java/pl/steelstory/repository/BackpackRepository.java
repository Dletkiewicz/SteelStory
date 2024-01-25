package pl.steelstory.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.steelstory.entity.BackpackEntity;

import java.util.UUID;

@Repository
public interface BackpackRepository extends JpaRepository<BackpackEntity, UUID> {

  @EntityGraph(attributePaths = {"character", "items"})
  BackpackEntity getByCharacterBusinessId(UUID id);

}
