package dev.haypacomer.domain.analytics;

import dev.haypacomer.domain.quantity.Grams;
import java.util.Objects;

public record WasteTip(String foodKey, int times, Grams discarded, int buyLessPercent) {

  public WasteTip {
    Objects.requireNonNull(foodKey, "foodKey");
    Objects.requireNonNull(discarded, "discarded");
  }

  public String text() {
    return "You threw away "
        + foodKey
        + " "
        + times
        + " times ("
        + discarded
        + "). Buy about "
        + buyLessPercent
        + "% less, or plan a dish for it earlier.";
  }
}
