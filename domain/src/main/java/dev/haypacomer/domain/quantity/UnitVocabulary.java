package dev.haypacomer.domain.quantity;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class UnitVocabulary {

  private static final Map<String, Unit> WORDS = new HashMap<>();

  static {
    register(Unit.MILLIGRAM, "mg", "milligram", "milligrams", "miligramo", "miligramos");
    register(Unit.GRAM, "g", "gr", "grs", "gram", "grams", "gramo", "gramos");
    register(
        Unit.KILOGRAM,
        "kg",
        "kgs",
        "kilo",
        "kilos",
        "kilogram",
        "kilograms",
        "kilogramo",
        "kilogramos");
    register(Unit.OUNCE, "oz", "ounce", "ounces", "onza", "onzas");
    register(Unit.POUND, "lb", "lbs", "pound", "pounds", "libra", "libras");
    register(
        Unit.MILLILITER,
        "ml",
        "cc",
        "milliliter",
        "milliliters",
        "millilitre",
        "millilitres",
        "mililitro",
        "mililitros");
    register(Unit.LITER, "l", "lt", "liter", "liters", "litre", "litres", "litro", "litros");
    register(
        Unit.TEASPOON,
        "tsp",
        "teaspoon",
        "teaspoons",
        "cdta",
        "cdtas",
        "cucharadita",
        "cucharaditas");
    register(
        Unit.TABLESPOON,
        "tbsp",
        "tablespoon",
        "tablespoons",
        "cda",
        "cdas",
        "cucharada",
        "cucharadas");
    register(Unit.CUP, "cup", "cups", "taza", "tazas");
    register(Unit.PIECE, "pc", "pcs", "piece", "pieces", "unit", "units", "unidad", "unidades");
  }

  private UnitVocabulary() {}

  private static void register(Unit unit, String... words) {
    for (String word : words) {
      WORDS.put(word, unit);
    }
  }

  public static Optional<Unit> lookup(String word) {
    if (word == null) {
      return Optional.empty();
    }
    String normalized = word.strip().toLowerCase(Locale.ROOT);
    if (normalized.endsWith(".")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return Optional.ofNullable(WORDS.get(normalized));
  }
}
