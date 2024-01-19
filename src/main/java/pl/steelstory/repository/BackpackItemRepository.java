package pl.steelstory.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.steelstory.entity.BackpackItemEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BackpackItemRepository extends JpaRepository<BackpackItemEntity, UUID> {

  @EntityGraph(attributePaths = {"character"})
  List<BackpackItemEntity> findAllByCharacterBusinessId(UUID characterId);

  @EntityGraph(attributePaths = {"character"})
  Optional<BackpackItemEntity> findByBusinessIdAndCharacterBusinessId(UUID id, UUID characterId);

}
