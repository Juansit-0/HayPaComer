package dev.haypacomer.domain.member;

import dev.haypacomer.domain.food.Allergen;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeRequirement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record FoodProfile(
    MemberId member, Diet diet, Set<Allergen> allergies, Set<String> avoidedFoods) {

  public FoodProfile {
    Objects.requireNonNull(member, "member");
    Objects.requireNonNull(diet, "diet");
    allergies = Set.copyOf(allergies);
    avoidedFoods =
        avoidedFoods.stream().map(FoodMetadata::keyOf).collect(Collectors.toUnmodifiableSet());
  }

  public static FoodProfile omnivore(MemberId member) {
    return new FoodProfile(member, Diet.OMNIVORE, Set.of(), Set.of());
  }

  public FoodProfile withDiet(Diet newDiet) {
    return new FoodProfile(member, newDiet, allergies, avoidedFoods);
  }

  public FoodProfile withAllergies(Set<Allergen> newAllergies) {
    return new FoodProfile(member, diet, newAllergies, avoidedFoods);
  }

  public FoodProfile avoiding(Set<String> foodNames) {
    return new FoodProfile(member, diet, allergies, foodNames);
  }

  public boolean allows(FoodMetadata food) {
    return conflictsWith(food).isEmpty();
  }

  public List<ProfileConflict> conflictsWith(FoodMetadata food) {
    List<ProfileConflict> conflicts = new ArrayList<>();
    food.allergens().stream()
        .filter(allergies::contains)
        .sorted()
        .forEach(
            allergen ->
                conflicts.add(
                    new ProfileConflict(member, food, ConflictReason.ALLERGY, allergen.name())));
    if (diet.excludes(food.category())) {
      conflicts.add(
          new ProfileConflict(
              member, food, ConflictReason.DIET, diet.name() + " excludes " + food.category()));
    }
    if (avoidedFoods.contains(food.key())) {
      conflicts.add(new ProfileConflict(member, food, ConflictReason.AVOIDED, food.name()));
    }
    return List.copyOf(conflicts);
  }

  public List<ProfileConflict> conflictsWith(Recipe recipe) {
    return recipe.requirements().stream()
        .map(RecipeRequirement::food)
        .flatMap(food -> conflictsWith(food).stream())
        .toList();
  }
}
