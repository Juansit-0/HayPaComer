package dev.haypacomer.web.cooking;

import dev.haypacomer.application.inventory.FoodNotInCatalogException;
import dev.haypacomer.application.port.FoodCatalogRepository;
import dev.haypacomer.application.quantity.InterpretQuantity;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeId;
import dev.haypacomer.domain.recipe.RecipeRequirement;
import dev.haypacomer.domain.recipe.RecipeSource;
import dev.haypacomer.domain.recipe.RecipeStep;
import dev.haypacomer.domain.recipe.StepWeighing;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RecipeAssembler {

  private final InterpretQuantity interpretQuantity;
  private final FoodCatalogRepository catalog;

  public RecipeAssembler(InterpretQuantity interpretQuantity, FoodCatalogRepository catalog) {
    this.interpretQuantity = interpretQuantity;
    this.catalog = catalog;
  }

  public Recipe assemble(
      String name,
      int servings,
      int minutes,
      List<RequirementRequest> requirements,
      List<StepRequest> steps) {
    List<RecipeStep> recipeSteps = new ArrayList<>();
    List<StepRequest> requested = steps == null ? List.of() : steps;
    for (int index = 0; index < requested.size(); index++) {
      StepRequest step = requested.get(index);
      RecipeStep recipeStep = RecipeStep.of(index + 1, step.instruction());
      if (step.timerSeconds() != null) {
        recipeStep = recipeStep.withTimer(Duration.ofSeconds(step.timerSeconds()));
      }
      if (step.weigh() != null) {
        recipeStep =
            recipeStep.withWeighing(
                new StepWeighing(food(step.weigh().food()), grams(step.weigh())));
      }
      recipeSteps.add(recipeStep);
    }
    return new Recipe(
        RecipeId.newId(),
        name,
        servings,
        minutes,
        RecipeSource.MANUAL,
        requirements.stream()
            .map(
                requirement ->
                    new RecipeRequirement(
                        food(requirement.food()),
                        grams(requirement),
                        Boolean.TRUE.equals(requirement.optional())))
            .toList(),
        recipeSteps);
  }

  private Grams grams(RequirementRequest requirement) {
    if (requirement.grams() != null) {
      return Grams.of(requirement.grams());
    }
    if (requirement.quantity() == null) {
      throw new IllegalArgumentException(
          "Requirement for " + requirement.food() + " needs grams or a quantity");
    }
    return interpretQuantity.interpret(requirement.food(), requirement.quantity()).grams();
  }

  private FoodMetadata food(String name) {
    return catalog.findByName(name).orElseThrow(() -> new FoodNotInCatalogException(name));
  }
}
