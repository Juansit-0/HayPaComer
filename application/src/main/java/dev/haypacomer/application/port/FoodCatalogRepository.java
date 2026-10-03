package dev.haypacomer.application.port;

import dev.haypacomer.domain.food.FoodMetadata;
import java.util.List;
import java.util.Optional;

public interface FoodCatalogRepository {

  void save(FoodMetadata food);

  Optional<FoodMetadata> findByName(String name);

  List<FoodMetadata> search(String text, int limit);
}
