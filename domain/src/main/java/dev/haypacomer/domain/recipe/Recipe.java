package dev.haypacomer.domain.recipe;

import dev.haypacomer.domain.food.Allergen;
import dev.haypacomer.domain.food.FoodMetadata;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record Recipe(
    RecipeId id,
    String name,
    int servings,
    int minutes,
    RecipeSource source,
    List<RecipeRequirement> requirements,
    List<RecipeStep> steps) {

  public Recipe {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(source, "source");
    name = name.strip();
    if (name.isEmpty()) {
      throw new IllegalArgumentException("Recipe name cannot be blank");
    }
    if (servings < 1) {
      throw new IllegalArgumentException("Servings must be at least 1: " + servings);
    }
    if (minutes < 1) {
      throw new IllegalArgumentException("Minutes must be at least 1: " + minutes);
    }
    requirements = List.copyOf(requirements);
    steps = List.copyOf(steps);
    requireUniqueFoods(requirements);
    requireSequentialSteps(steps);
    requireWeighedFoodsAreRequired(requirements, steps);
  }

  public Optional<RecipeRequirement> requirementFor(FoodMetadata food) {
    return requirements.stream()
        .filter(requirement -> requirement.food().key().equals(food.key()))
        .findFirst();
  }

  public List<RecipeRequirement> mandatoryRequirements() {
    return requirements.stream().filter(requirement -> !requirement.optional()).toList();
  }

  public Set<Allergen> allergens() {
    Set<Allergen> allergens = EnumSet.noneOf(Allergen.class);
    requirements.forEach(requirement -> allergens.addAll(requirement.food().allergens()));
    return Set.copyOf(allergens);
  }

  public Recipe scaledTo(int targetServings) {
    if (targetServings < 1) {
      throw new IllegalArgumentException("Servings must be at least 1: " + targetServings);
    }
    BigDecimal factor =
        BigDecimal.valueOf(targetServings)
            .divide(BigDecimal.valueOf(servings), MathContext.DECIMAL64);
    return new Recipe(
        id,
        name,
        targetServings,
        minutes,
        source,
        requirements.stream().map(requirement -> requirement.scaledBy(factor)).toList(),
        steps.stream().map(step -> step.scaledBy(factor)).toList());
  }

  private static void requireUniqueFoods(List<RecipeRequirement> requirements) {
    Set<String> keys = new HashSet<>();
    for (RecipeRequirement requirement : requirements) {
      if (!keys.add(requirement.food().key())) {
        throw new IllegalArgumentException(
            "Duplicate requirement for " + requirement.food().name());
      }
    }
  }

  private static void requireSequentialSteps(List<RecipeStep> steps) {
    for (int index = 0; index < steps.size(); index++) {
      if (steps.get(index).position() != index + 1) {
        throw new IllegalArgumentException("Steps must be numbered 1.." + steps.size());
      }
    }
  }

  private static void requireWeighedFoodsAreRequired(
      List<RecipeRequirement> requirements, List<RecipeStep> steps) {
    Set<String> required = new HashSet<>();
    requirements.forEach(requirement -> required.add(requirement.food().key()));
    steps.stream()
        .flatMap(step -> step.weighingTarget().stream())
        .filter(weighing -> !required.contains(weighing.food().key()))
        .findFirst()
        .ifPresent(
            weighing -> {
              throw new IllegalArgumentException(
                  "Step weighs " + weighing.food().name() + " which is not a requirement");
            });
  }
}
