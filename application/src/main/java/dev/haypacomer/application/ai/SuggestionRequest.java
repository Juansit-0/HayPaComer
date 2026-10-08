package dev.haypacomer.application.ai;

import dev.haypacomer.domain.cooking.Availability;
import dev.haypacomer.domain.member.DiningGroup;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.substitution.SubstitutionCatalog;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record SuggestionRequest(
    List<Recipe> candidates,
    Availability availability,
    Set<String> atRiskFoods,
    DiningGroup diners,
    SubstitutionCatalog substitutions,
    int servings,
    Integer maxMinutes,
    int limit) {

  public static final int MAX_SUGGESTIONS = 3;

  public SuggestionRequest {
    Objects.requireNonNull(availability, "availability");
    Objects.requireNonNull(diners, "diners");
    Objects.requireNonNull(substitutions, "substitutions");
    candidates = List.copyOf(candidates);
    atRiskFoods = Set.copyOf(atRiskFoods);
    if (servings < 1) {
      throw new IllegalArgumentException("Servings must be at least 1: " + servings);
    }
    if (maxMinutes != null && maxMinutes < 1) {
      throw new IllegalArgumentException("Minutes must be at least 1: " + maxMinutes);
    }
    if (limit < 1 || limit > MAX_SUGGESTIONS) {
      throw new IllegalArgumentException("Limit must be between 1 and " + MAX_SUGGESTIONS);
    }
  }

  public Optional<Integer> minutesLimit() {
    return Optional.ofNullable(maxMinutes);
  }
}
