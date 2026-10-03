package dev.haypacomer.domain.member;

import dev.haypacomer.domain.food.FoodCategory;
import java.util.EnumSet;
import java.util.Set;

public enum Diet {
  OMNIVORE(EnumSet.noneOf(FoodCategory.class)),
  PESCATARIAN(EnumSet.of(FoodCategory.MEAT, FoodCategory.POULTRY)),
  VEGETARIAN(EnumSet.of(FoodCategory.MEAT, FoodCategory.POULTRY, FoodCategory.FISH)),
  VEGAN(
      EnumSet.of(
          FoodCategory.MEAT,
          FoodCategory.POULTRY,
          FoodCategory.FISH,
          FoodCategory.DAIRY,
          FoodCategory.EGGS));

  private final Set<FoodCategory> excluded;

  Diet(Set<FoodCategory> excluded) {
    this.excluded = Set.copyOf(excluded);
  }

  public Set<FoodCategory> excludedCategories() {
    return excluded;
  }

  public boolean excludes(FoodCategory category) {
    return excluded.contains(category);
  }
}
