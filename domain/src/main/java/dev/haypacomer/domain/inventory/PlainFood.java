package dev.haypacomer.domain.inventory;

import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.member.MemberId;
import java.util.Objects;
import java.util.Set;

public record PlainFood(FoodItem item) implements StockedFood {

  public PlainFood {
    Objects.requireNonNull(item, "item");
  }

  @Override
  public Set<FoodStatus> statuses() {
    return Set.of();
  }

  @Override
  public int rescuePriority() {
    return 0;
  }

  @Override
  public boolean isEdible() {
    return true;
  }

  @Override
  public boolean isUsableBy(MemberId member) {
    return true;
  }
}
