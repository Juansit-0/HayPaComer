package dev.haypacomer.domain.food;

import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Quantity;
import dev.haypacomer.domain.quantity.Unit;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public record FoodMetadata(
    String name,
    FoodCategory category,
    Unit defaultUnit,
    ConversionFactors conversion,
    boolean perishable,
    int shelfDays,
    Set<Allergen> allergens) {

  public FoodMetadata {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(category, "category");
    Objects.requireNonNull(defaultUnit, "defaultUnit");
    Objects.requireNonNull(conversion, "conversion");
    Objects.requireNonNull(allergens, "allergens");
    name = name.strip();
    if (name.isEmpty()) {
      throw new IllegalArgumentException("Food name cannot be blank");
    }
    if (shelfDays < 0) {
      throw new IllegalArgumentException("Shelf days cannot be negative: " + shelfDays);
    }
    allergens = Set.copyOf(allergens);
  }

  public static String keyOf(String name) {
    return name.strip().toLowerCase(Locale.ROOT);
  }

  public String key() {
    return keyOf(name);
  }

  public boolean sameDefinitionAs(FoodMetadata other) {
    return key().equals(other.key())
        && category == other.category
        && defaultUnit == other.defaultUnit
        && conversion.equals(other.conversion)
        && perishable == other.perishable
        && shelfDays == other.shelfDays
        && allergens.equals(other.allergens);
  }

  public boolean contains(Allergen allergen) {
    return allergens.contains(allergen);
  }

  public Grams toGrams(Quantity quantity) {
    return quantity.toGrams(conversion);
  }
}
