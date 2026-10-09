package dev.haypacomer.domain.analytics;

import dev.haypacomer.domain.inventory.InventoryMovement;
import dev.haypacomer.domain.inventory.MovementType;
import dev.haypacomer.domain.quantity.Grams;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WastePatterns {

  public static final int MIN_TIMES = 2;
  public static final int MAX_TIPS = 3;

  private WastePatterns() {}

  public static List<WasteTip> find(List<InventoryMovement> movements) {
    Map<String, Integer> times = new HashMap<>();
    Map<String, Grams> discarded = new HashMap<>();
    Map<String, Grams> consumed = new HashMap<>();
    for (InventoryMovement movement : movements) {
      String food = movement.food().orElse(null);
      if (food == null) {
        continue;
      }
      Grams grams = Grams.of(movement.deltaGrams().abs());
      if (movement.type() == MovementType.DISCARD) {
        times.merge(food, 1, Integer::sum);
        discarded.merge(food, grams, Grams::plus);
      } else if (movement.type() == MovementType.CONSUME) {
        consumed.merge(food, grams, Grams::plus);
      }
    }
    return times.entrySet().stream()
        .filter(entry -> entry.getValue() >= MIN_TIMES)
        .map(
            entry -> {
              Grams wasted = discarded.get(entry.getKey());
              Grams eaten = consumed.getOrDefault(entry.getKey(), Grams.ZERO);
              return new WasteTip(entry.getKey(), entry.getValue(), wasted, buyLess(wasted, eaten));
            })
        .sorted(
            Comparator.comparing(WasteTip::discarded).reversed().thenComparing(WasteTip::foodKey))
        .limit(MAX_TIPS)
        .toList();
  }

  private static int buyLess(Grams wasted, Grams eaten) {
    BigDecimal total = wasted.value().add(eaten.value());
    int percent =
        wasted
            .value()
            .multiply(BigDecimal.valueOf(100))
            .divide(total, 0, RoundingMode.HALF_UP)
            .intValue();
    int rounded = Math.max(10, Math.round(percent / 10f) * 10);
    return Math.min(rounded, 90);
  }
}
