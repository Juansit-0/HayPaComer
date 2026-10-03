package dev.haypacomer.domain.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.food.Allergen;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecipeTest {

  static final FoodMetadata CHICKEN = food("Chicken breast", FoodCategory.POULTRY, Set.of());
  static final FoodMetadata RICE = food("Rice", FoodCategory.GRAIN, Set.of());
  static final FoodMetadata SOY_SAUCE =
      food("Soy sauce", FoodCategory.CONDIMENT, Set.of(Allergen.SOY, Allergen.GLUTEN));

  static FoodMetadata food(String name, FoodCategory category, Set<Allergen> allergens) {
    return new FoodMetadata(
        name, category, Unit.GRAM, ConversionFactors.MASS_ONLY, true, 3, allergens);
  }

  static Recipe riceWithChicken() {
    return new Recipe(
        RecipeId.newId(),
        "Rice with chicken",
        2,
        35,
        RecipeSource.MANUAL,
        List.of(
            RecipeRequirement.of(CHICKEN, Grams.of(200)),
            RecipeRequirement.of(RICE, Grams.of(150)),
            RecipeRequirement.optional(SOY_SAUCE, Grams.of(15))),
        List.of(
            RecipeStep.of(1, "Weigh the chicken")
                .withWeighing(new StepWeighing(CHICKEN, Grams.of(200))),
            RecipeStep.of(2, "Cook the rice").withTimer(Duration.ofMinutes(18)),
            RecipeStep.of(3, "Mix and serve")));
  }

  @Test
  void findsRequirementByFood() {
    Recipe recipe = riceWithChicken();

    assertEquals(Grams.of(200), recipe.requirementFor(CHICKEN).orElseThrow().grams());
    assertTrue(recipe.requirementFor(food("Tuna", FoodCategory.FISH, Set.of())).isEmpty());
  }

  @Test
  void separatesMandatoryFromOptional() {
    List<RecipeRequirement> mandatory = riceWithChicken().mandatoryRequirements();

    assertEquals(2, mandatory.size());
    assertFalse(mandatory.stream().anyMatch(RecipeRequirement::optional));
  }

  @Test
  void collectsAllergensFromRequirements() {
    assertEquals(Set.of(Allergen.SOY, Allergen.GLUTEN), riceWithChicken().allergens());
  }

  @Test
  void scalesRequirementsAndWeighingTargetsToServings() {
    Recipe forOne = riceWithChicken().scaledTo(1);

    assertEquals(1, forOne.servings());
    assertEquals(Grams.of(100), forOne.requirementFor(CHICKEN).orElseThrow().grams());
    assertEquals(Grams.of(75), forOne.requirementFor(RICE).orElseThrow().grams());
    assertEquals(Grams.of(100), forOne.steps().getFirst().weighingTarget().orElseThrow().target());
    assertEquals(Duration.ofMinutes(18), forOne.steps().get(1).timerDuration().orElseThrow());
  }

  @Test
  void scalesUpForMorePeople() {
    Recipe forThree = riceWithChicken().scaledTo(3);

    assertEquals(Grams.of(300), forThree.requirementFor(CHICKEN).orElseThrow().grams());
    assertEquals(Grams.of("22.5"), forThree.requirementFor(SOY_SAUCE).orElseThrow().grams());
  }

  @Test
  void rejectsInvalidServingsAndMinutes() {
    Recipe recipe = riceWithChicken();

    assertThrows(IllegalArgumentException.class, () -> recipe.scaledTo(0));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Recipe(RecipeId.newId(), "Soup", 0, 10, RecipeSource.MANUAL, List.of(), List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Recipe(RecipeId.newId(), "Soup", 1, 0, RecipeSource.MANUAL, List.of(), List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () -> new Recipe(RecipeId.newId(), " ", 1, 10, RecipeSource.MANUAL, List.of(), List.of()));
  }

  @Test
  void rejectsDuplicateRequirements() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Recipe(
                RecipeId.newId(),
                "Double rice",
                1,
                20,
                RecipeSource.MANUAL,
                List.of(
                    RecipeRequirement.of(RICE, Grams.of(100)),
                    RecipeRequirement.of(RICE, Grams.of(50))),
                List.of()));
  }

  @Test
  void rejectsStepsOutOfOrder() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Recipe(
                RecipeId.newId(),
                "Rice",
                1,
                20,
                RecipeSource.MANUAL,
                List.of(RecipeRequirement.of(RICE, Grams.of(100))),
                List.of(RecipeStep.of(2, "Cook"))));
  }

  @Test
  void rejectsWeighingOfFoodNotRequired() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Recipe(
                RecipeId.newId(),
                "Rice",
                1,
                20,
                RecipeSource.MANUAL,
                List.of(RecipeRequirement.of(RICE, Grams.of(100))),
                List.of(
                    RecipeStep.of(1, "Weigh chicken")
                        .withWeighing(new StepWeighing(CHICKEN, Grams.of(100))))));
  }

  @Test
  void exposesImmutableLists() {
    Recipe recipe = riceWithChicken();

    assertThrows(UnsupportedOperationException.class, () -> recipe.steps().clear());
    assertThrows(UnsupportedOperationException.class, () -> recipe.requirements().clear());
    assertEquals(RecipeSource.MANUAL, recipe.source());
  }
}
