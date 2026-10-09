package dev.haypacomer.domain.analytics;

import dev.haypacomer.domain.quantity.Grams;
import java.math.BigDecimal;
import java.util.Objects;

public record Tally(Grams consumed, Grams rescued, Grams discarded) {

  public static final Tally EMPTY = new Tally(Grams.ZERO, Grams.ZERO, Grams.ZERO);

  public Tally {
    Objects.requireNonNull(consumed, "consumed");
    Objects.requireNonNull(rescued, "rescued");
    Objects.requireNonNull(discarded, "discarded");
  }

  public Tally plus(Tally other) {
    return new Tally(
        consumed.plus(other.consumed),
        rescued.plus(other.rescued),
        discarded.plus(other.discarded));
  }

  public double wasteRate() {
    BigDecimal total = consumed.value().add(discarded.value());
    if (total.signum() == 0) {
      return 0;
    }
    return discarded.value().doubleValue() / total.doubleValue();
  }
}
