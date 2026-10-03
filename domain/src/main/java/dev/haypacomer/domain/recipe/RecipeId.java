package dev.haypacomer.domain.recipe;

import java.util.Objects;
import java.util.UUID;

public record RecipeId(UUID value) {

  public RecipeId {
    Objects.requireNonNull(value, "value");
  }

  public static RecipeId newId() {
    return new RecipeId(UUID.randomUUID());
  }
}
