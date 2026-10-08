package dev.haypacomer.domain.substitution;

import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.quantity.Grams;
import java.math.BigDecimal;
import java.util.Objects;

public record SubstitutionRule(
    SubstitutionRuleId id,
    FoodMetadata original,
    FoodMetadata substitute,
    BigDecimal ratio,
    Grams maxReplaced) {

  public SubstitutionRule {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(original, "original");
    Objects.requireNonNull(substitute, "substitute");
    Objects.requireNonNull(ratio, "ratio");
    Objects.requireNonNull(maxReplaced, "maxReplaced");
    if (original.key().equals(substitute.key())) {
      throw new IllegalArgumentException("A food cannot substitute itself: " + original.name());
    }
    if (ratio.signum() <= 0) {
      throw new IllegalArgumentException("Ratio must be positive: " + ratio);
    }
    if (maxReplaced.isZero()) {
      throw new IllegalArgumentException("Replaced limit must be positive");
    }
  }

  public static SubstitutionRule of(
      FoodMetadata original, FoodMetadata substitute, String ratio, long maxReplacedGrams) {
    return new SubstitutionRule(
        SubstitutionRuleId.newId(),
        original,
        substitute,
        new BigDecimal(ratio),
        Grams.of(maxReplacedGrams));
  }

  public boolean replaces(FoodMetadata food) {
    return original.key().equals(food.key());
  }

  public boolean allowsReplacing(Grams grams) {
    return maxReplaced.isAtLeast(grams);
  }

  public Grams substituteGramsFor(Grams replaced) {
    return replaced.times(ratio);
  }
}
