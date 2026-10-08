package dev.haypacomer.persistence.relational;

import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeId;
import dev.haypacomer.domain.recipe.RecipeRequirement;
import dev.haypacomer.domain.recipe.RecipeSource;
import dev.haypacomer.domain.recipe.RecipeStep;
import dev.haypacomer.domain.recipe.StepWeighing;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import tools.jackson.databind.json.JsonMapper;

record RecipePayload(
    UUID id,
    String name,
    int servings,
    int minutes,
    RecipeSource source,
    List<RequirementJson> requirements,
    List<StepJson> steps) {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  static String write(Recipe recipe) {
    return JSON.writeValueAsString(
        new RecipePayload(
            recipe.id().value(),
            recipe.name(),
            recipe.servings(),
            recipe.minutes(),
            recipe.source(),
            recipe.requirements().stream().map(RequirementJson::from).toList(),
            recipe.steps().stream().map(StepJson::from).toList()));
  }

  static RecipePayload read(String json) {
    return JSON.readValue(json, RecipePayload.class);
  }

  Set<String> foodKeys() {
    Set<String> keys = new HashSet<>();
    requirements.forEach(requirement -> keys.add(requirement.food()));
    steps.stream()
        .filter(step -> step.weighFood() != null)
        .forEach(step -> keys.add(step.weighFood()));
    return keys;
  }

  Recipe toRecipe(Map<String, FoodMetadata> foodsByKey) {
    Function<String, FoodMetadata> food =
        key -> {
          FoodMetadata found = foodsByKey.get(key);
          if (found == null) {
            throw new IllegalStateException("Recipe food is no longer in the catalog: " + key);
          }
          return found;
        };
    return new Recipe(
        new RecipeId(id),
        name,
        servings,
        minutes,
        source,
        requirements.stream()
            .map(
                requirement ->
                    new RecipeRequirement(
                        food.apply(requirement.food()),
                        Grams.of(requirement.grams()),
                        requirement.optional()))
            .toList(),
        steps.stream().map(step -> step.toStep(food)).toList());
  }

  record RequirementJson(String food, BigDecimal grams, boolean optional) {

    static RequirementJson from(RecipeRequirement requirement) {
      return new RequirementJson(
          requirement.food().key(), requirement.grams().value(), requirement.optional());
    }
  }

  record StepJson(
      int position,
      String instruction,
      Long timerSeconds,
      String weighFood,
      BigDecimal weighGrams) {

    static StepJson from(RecipeStep step) {
      return new StepJson(
          step.position(),
          step.instruction(),
          step.timerDuration().map(Duration::toSeconds).orElse(null),
          step.weighingTarget().map(weighing -> weighing.food().key()).orElse(null),
          step.weighingTarget().map(weighing -> weighing.target().value()).orElse(null));
    }

    RecipeStep toStep(Function<String, FoodMetadata> food) {
      RecipeStep step = RecipeStep.of(position, instruction);
      if (timerSeconds != null) {
        step = step.withTimer(Duration.ofSeconds(timerSeconds));
      }
      if (weighFood != null) {
        step = step.withWeighing(new StepWeighing(food.apply(weighFood), Grams.of(weighGrams)));
      }
      return step;
    }
  }
}
