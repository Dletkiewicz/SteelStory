package pl.steelstory.repository;

import org.springframework.stereotype.Repository;
import pl.steelstory.entity.ItemEntity;

import java.util.Optional;

@Repository
public interface ItemRepository {


  Optional<ItemEntity> findByCharacter
}
