package dev.haypacomer.application.ai;

import dev.haypacomer.domain.quantity.Grams;
import java.util.Objects;
import java.util.Optional;

public record DraftIngredient(
    String readFood, String readQuantity, String catalogFood, Grams grams, String problem) {

  public DraftIngredient {
    Objects.requireNonNull(readFood, "readFood");
    Objects.requireNonNull(readQuantity, "readQuantity");
  }

  public boolean verified() {
    return catalogFood != null && grams != null && problem == null;
  }

  public Optional<String> issue() {
    return Optional.ofNullable(problem);
  }
}
