package pl.steelstory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.steelstory.entity.ItemEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItemRepository extends JpaRepository<ItemEntity, UUID> {

  Optional<ItemEntity> findByBusinessId(UUID id);
}
