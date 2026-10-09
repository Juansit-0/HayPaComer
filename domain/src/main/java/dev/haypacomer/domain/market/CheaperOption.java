package dev.haypacomer.domain.market;

import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.quantity.Grams;
import java.math.BigDecimal;
import java.util.Objects;

public record CheaperOption(FoodMetadata substitute, Grams grams, BigDecimal estimatedCost) {

  public CheaperOption {
    Objects.requireNonNull(substitute, "substitute");
    Objects.requireNonNull(grams, "grams");
    Objects.requireNonNull(estimatedCost, "estimatedCost");
  }
}
