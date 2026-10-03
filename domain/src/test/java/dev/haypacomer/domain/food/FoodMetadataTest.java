package dev.haypacomer.domain.food;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Quantity;
import dev.haypacomer.domain.quantity.Unit;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FoodMetadataTest {

  static FoodMetadata milk(String name) {
    return new FoodMetadata(
        name,
        FoodCategory.DAIRY,
        Unit.MILLILITER,
        ConversionFactors.withDensity("1.03"),
        true,
        7,
        EnumSet.of(Allergen.MILK));
  }

  @Test
  void stripsNameAndBuildsCaseInsensitiveKey() {
    FoodMetadata food = milk("  Whole Milk ");

    assertEquals("Whole Milk", food.name());
    assertEquals("whole milk", food.key());
  }

  @Test
  void convertsQuantityWithItsOwnFactors() {
    assertEquals(Grams.of(206), milk("Milk").toGrams(Quantity.of("200", Unit.MILLILITER)));
  }

  @Test
  void reportsAllergens() {
    FoodMetadata food = milk("Milk");

    assertTrue(food.contains(Allergen.MILK));
    assertFalse(food.contains(Allergen.GLUTEN));
  }

  @Test
  void allergensAreImmutableCopies() {
    Set<Allergen> allergens = new HashSet<>(Set.of(Allergen.FISH));
    FoodMetadata tuna =
        new FoodMetadata(
            "Tuna", FoodCategory.FISH, Unit.GRAM, ConversionFactors.MASS_ONLY, true, 3, allergens);

    allergens.add(Allergen.SOY);

    assertFalse(tuna.contains(Allergen.SOY));
    assertThrows(UnsupportedOperationException.class, () -> tuna.allergens().add(Allergen.EGGS));
  }

  @Test
  void rejectsBlankName() {
    assertThrows(IllegalArgumentException.class, () -> milk("   "));
  }

  @Test
  void rejectsNegativeShelfDays() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new FoodMetadata(
                "Rice",
                FoodCategory.GRAIN,
                Unit.GRAM,
                ConversionFactors.MASS_ONLY,
                false,
                -1,
                Set.of()));
  }

  @Test
  void comparesDefinitionIgnoringNameCase() {
    assertTrue(milk("Milk").sameDefinitionAs(milk("milk")));
    assertFalse(milk("Milk").sameDefinitionAs(milk("Oat milk")));
  }
}
