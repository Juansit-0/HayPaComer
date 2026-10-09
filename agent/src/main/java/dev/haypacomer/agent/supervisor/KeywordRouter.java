package dev.haypacomer.agent.supervisor;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class KeywordRouter implements SpecialistRouter {

  public static final int MAX_SPECIALISTS = 2;

  private static final Map<Specialist, List<String>> KEYWORDS = new LinkedHashMap<>();

  static {
    KEYWORDS.put(
        Specialist.COLD,
        List.of(
            "temperature",
            "cold",
            "warm",
            "door",
            "fridge broke",
            "power",
            "temperatura",
            "frio",
            "caliente",
            "puerta",
            "luz",
            "nevera"));
    KEYWORDS.put(
        Specialist.MARKET,
        List.of(
            "buy",
            "market",
            "shopping",
            "groceries",
            "store",
            "compr",
            "mercado",
            "lista",
            "tienda",
            "surtir"));
    KEYWORDS.put(
        Specialist.COACH,
        List.of(
            "waste",
            "save",
            "habit",
            "tip",
            "throw away",
            "desperdicio",
            "botar",
            "ahorrar",
            "habito",
            "consejo"));
    KEYWORDS.put(
        Specialist.CHEF,
        List.of(
            "cook",
            "recipe",
            "dinner",
            "lunch",
            "breakfast",
            "eat",
            "meal",
            "cocinar",
            "receta",
            "cena",
            "almuerzo",
            "desayuno",
            "comer",
            "comida"));
  }

  @Override
  public List<Specialist> route(String goal) {
    String text = normalize(goal);
    List<Specialist> chosen = new ArrayList<>();
    KEYWORDS.forEach(
        (specialist, words) -> {
          if (chosen.size() < MAX_SPECIALISTS && words.stream().anyMatch(text::contains)) {
            chosen.add(specialist);
          }
        });
    return chosen.isEmpty() ? List.of(Specialist.CHEF) : List.copyOf(chosen);
  }

  private static String normalize(String text) {
    return Normalizer.normalize(text, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT);
  }
}
