package dev.haypacomer.persistence.resilience;

import dev.haypacomer.application.port.FoodCatalogRepository;
import dev.haypacomer.domain.food.FoodMetadata;
import java.util.List;
import java.util.Optional;

public final class ResilientFoodCatalog implements FoodCatalogRepository {

  private final FoodCatalogRepository delegate;
  private final ResilientReads reads;

  public ResilientFoodCatalog(FoodCatalogRepository delegate, ResilientReads reads) {
    this.delegate = delegate;
    this.reads = reads;
  }

  @Override
  public void save(FoodMetadata food) {
    reads.write(
        () -> {
          delegate.save(food);
          return food;
        });
  }

  @Override
  public Optional<FoodMetadata> findByName(String name) {
    return reads.read("name:" + FoodMetadata.keyOf(name), () -> delegate.findByName(name));
  }

  @Override
  public List<FoodMetadata> search(String text, int limit) {
    return reads.read(
        "search:" + FoodMetadata.keyOf(text) + ":" + limit, () -> delegate.search(text, limit));
  }
}
