package dev.haypacomer.domain.fridge;

import dev.haypacomer.domain.food.Allergen;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import java.time.LocalDate;
import java.util.Set;

final class FridgeFixtures {

  static final FoodMetadata MILK =
      new FoodMetadata(
          "Milk",
          FoodCategory.DAIRY,
          Unit.MILLILITER,
          ConversionFactors.withDensity("1.03"),
          true,
          7,
          Set.of(Allergen.MILK));

  static final FoodMetadata CHICKEN =
      new FoodMetadata(
          "Chicken breast",
          FoodCategory.POULTRY,
          Unit.GRAM,
          ConversionFactors.MASS_ONLY,
          true,
          2,
          Set.of());

  private FridgeFixtures() {}

  static FoodItem item(FoodMetadata food, long grams) {
    return new FoodItem(
        FoodItemId.newId(), food, Grams.of(grams), Grams.ZERO, LocalDate.of(2026, 10, 5));
  }
}
