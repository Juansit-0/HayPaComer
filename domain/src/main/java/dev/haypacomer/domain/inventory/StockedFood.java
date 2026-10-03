package dev.haypacomer.domain.inventory;

import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.member.MemberId;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Set;

public interface StockedFood {

  Comparator<StockedFood> RESCUE_ORDER =
      Comparator.comparing(StockedFood::isEdible)
          .reversed()
          .thenComparing(Comparator.comparingInt(StockedFood::rescuePriority).reversed())
          .thenComparing(food -> food.item().expiresOn().orElse(LocalDate.MAX));

  FoodItem item();

  Set<FoodStatus> statuses();

  int rescuePriority();

  boolean isEdible();

  boolean isUsableBy(MemberId member);

  default boolean has(FoodStatus status) {
    return statuses().contains(status);
  }

  default String describe() {
    String base = item().name() + " " + item().quantity();
    return statuses().isEmpty() ? base : base + " " + statuses();
  }
}
