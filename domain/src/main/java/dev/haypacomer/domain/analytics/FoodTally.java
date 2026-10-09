package dev.haypacomer.domain.analytics;

import java.util.Objects;

public record FoodTally(String foodKey, Tally tally) {

  public FoodTally {
    Objects.requireNonNull(foodKey, "foodKey");
    Objects.requireNonNull(tally, "tally");
  }
}
