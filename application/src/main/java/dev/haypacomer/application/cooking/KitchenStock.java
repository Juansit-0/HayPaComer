package dev.haypacomer.application.cooking;

import dev.haypacomer.domain.cooking.Availability;
import java.util.Objects;
import java.util.Set;

public record KitchenStock(Availability availability, Set<String> atRiskFoods) {

  public KitchenStock {
    Objects.requireNonNull(availability, "availability");
    atRiskFoods = Set.copyOf(atRiskFoods);
  }
}
