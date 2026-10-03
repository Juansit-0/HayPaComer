package dev.haypacomer.domain.member;

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
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeId;
import dev.haypacomer.domain.recipe.RecipeRequirement;
import dev.haypacomer.domain.recipe.RecipeSource;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FoodProfileTest {

  static final FoodMetadata TUNA = food("Tuna", FoodCategory.FISH, Set.of(Allergen.FISH));
  static final FoodMetadata CHICKEN = food("Chicken breast", FoodCategory.POULTRY, Set.of());
  static final FoodMetadata RICE = food("Rice", FoodCategory.GRAIN, Set.of());
  static final FoodMetadata CHEESE = food("Cheese", FoodCategory.DAIRY, Set.of(Allergen.MILK));
  static final FoodMetadata CILANTRO = food("Cilantro", FoodCategory.VEGETABLE, Set.of());

  static FoodMetadata food(String name, FoodCategory category, Set<Allergen> allergens) {
    return new FoodMetadata(
        name, category, Unit.GRAM, ConversionFactors.MASS_ONLY, true, 3, allergens);
  }

  static Recipe recipe(FoodMetadata... foods) {
    return new Recipe(
        RecipeId.newId(),
        "Test dish",
        2,
        20,
        RecipeSource.MANUAL,
        List.of(foods).stream().map(food -> RecipeRequirement.of(food, Grams.of(100))).toList(),
        List.of());
  }

  @Test
  void omnivoreWithoutAllergiesAllowsEverything() {
    FoodProfile profile = FoodProfile.omnivore(MemberId.newId());

    assertTrue(profile.allows(TUNA));
    assertTrue(profile.allows(CHEESE));
  }

  @Test
  void allergyBlocksFoodContainingTheAllergen() {
    MemberId ana = MemberId.newId();
    FoodProfile profile = FoodProfile.omnivore(ana).withAllergies(Set.of(Allergen.FISH));

    assertEquals(
        List.of(new ProfileConflict(ana, TUNA, ConflictReason.ALLERGY, "FISH")),
        profile.conflictsWith(TUNA));
    assertTrue(profile.allows(CHICKEN));
  }

  @Test
  void dietExcludesCategories() {
    FoodProfile vegetarian = FoodProfile.omnivore(MemberId.newId()).withDiet(Diet.VEGETARIAN);
    FoodProfile vegan = FoodProfile.omnivore(MemberId.newId()).withDiet(Diet.VEGAN);
    FoodProfile pescatarian = FoodProfile.omnivore(MemberId.newId()).withDiet(Diet.PESCATARIAN);

    assertFalse(vegetarian.allows(CHICKEN));
    assertTrue(vegetarian.allows(CHEESE));
    assertFalse(vegan.allows(CHEESE));
    assertTrue(pescatarian.allows(TUNA));
    assertFalse(pescatarian.allows(CHICKEN));
    assertEquals(ConflictReason.DIET, vegetarian.conflictsWith(TUNA).getFirst().reason());
  }

  @Test
  void avoidedFoodsMatchIgnoringCase() {
    FoodProfile profile = FoodProfile.omnivore(MemberId.newId()).avoiding(Set.of(" CILANTRO "));

    assertEquals(ConflictReason.AVOIDED, profile.conflictsWith(CILANTRO).getFirst().reason());
    assertTrue(profile.avoidedFoods().contains("cilantro"));
  }

  @Test
  void reportsEveryReasonForOneFood() {
    FoodProfile profile =
        new FoodProfile(MemberId.newId(), Diet.VEGAN, Set.of(Allergen.MILK), Set.of("Cheese"));

    assertEquals(
        List.of(ConflictReason.ALLERGY, ConflictReason.DIET, ConflictReason.AVOIDED),
        profile.conflictsWith(CHEESE).stream().map(ProfileConflict::reason).toList());
  }

  @Test
  void checksEveryRequirementOfARecipe() {
    FoodProfile vegetarian = FoodProfile.omnivore(MemberId.newId()).withDiet(Diet.VEGETARIAN);

    List<ProfileConflict> conflicts = vegetarian.conflictsWith(recipe(CHICKEN, RICE, TUNA));

    assertEquals(List.of(CHICKEN, TUNA), conflicts.stream().map(ProfileConflict::food).toList());
  }

  @Test
  void collectionsAreImmutable() {
    FoodProfile profile =
        FoodProfile.omnivore(MemberId.newId()).withAllergies(Set.of(Allergen.PEANUTS));

    assertThrows(UnsupportedOperationException.class, () -> profile.allergies().clear());
    assertThrows(UnsupportedOperationException.class, () -> profile.avoidedFoods().add("rice"));
    assertEquals(
        Set.of(FoodCategory.MEAT, FoodCategory.POULTRY), Diet.PESCATARIAN.excludedCategories());
  }

  @Test
  void requiresMemberAndDiet() {
    assertThrows(NullPointerException.class, () -> FoodProfile.omnivore(null));
    assertThrows(
        NullPointerException.class,
        () -> new FoodProfile(MemberId.newId(), null, Set.of(), Set.of()));
    assertThrows(
        NullPointerException.class, () -> new ProfileConflict(MemberId.newId(), RICE, null, "x"));
  }
}
