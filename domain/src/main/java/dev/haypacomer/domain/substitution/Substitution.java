package dev.haypacomer.domain.substitution;

import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.quantity.Grams;
import java.util.Objects;

public record Substitution(SubstitutionRule rule, Grams replaced, Grams substituteGrams) {

  public Substitution {
    Objects.requireNonNull(rule, "rule");
    Objects.requireNonNull(replaced, "replaced");
    Objects.requireNonNull(substituteGrams, "substituteGrams");
  }

  public FoodMetadata substitute() {
    return rule.substitute();
  }
}
