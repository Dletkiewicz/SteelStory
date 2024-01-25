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

  @EntityGraph(attributePaths = {"backpack"})
  List<BackpackItemEntity> getAllByBackpackBusinessId(UUID backpackId);

  @EntityGraph(attributePaths = {"backpack"})
  Optional<BackpackItemEntity> findByBusinessIdAndBackpackBusinessId(UUID id, UUID backpackId);

  void deleteByBusinessId(UUID id);
}
