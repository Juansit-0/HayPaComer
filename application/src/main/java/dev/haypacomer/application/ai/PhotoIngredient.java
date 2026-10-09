package dev.haypacomer.application.ai;

import java.util.Objects;

public record PhotoIngredient(String food, String quantity) {

  public PhotoIngredient {
    Objects.requireNonNull(food, "food");
    Objects.requireNonNull(quantity, "quantity");
  }
}
