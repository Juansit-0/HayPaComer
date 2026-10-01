package dev.haypacomer.domain.food;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class FoodMetadataCatalog {

  private final Map<String, FoodMetadata> shared = new ConcurrentHashMap<>();

  public FoodMetadata intern(FoodMetadata candidate) {
    FoodMetadata existing = shared.putIfAbsent(candidate.key(), candidate);
    if (existing == null) {
      return candidate;
    }
    if (!existing.sameDefinitionAs(candidate)) {
      throw new IllegalArgumentException(
          "Conflicting metadata for food '" + candidate.name() + "'");
    }
    return existing;
  }

  public Optional<FoodMetadata> find(String name) {
    return Optional.ofNullable(shared.get(FoodMetadata.keyOf(name)));
  }

  public int size() {
    return shared.size();
  }
}
