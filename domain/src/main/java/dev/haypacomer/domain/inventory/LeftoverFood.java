package dev.haypacomer.domain.inventory;

import java.util.Set;

public final class LeftoverFood extends StockedFoodDecorator {

  private static final int RESCUE_BONUS = 1;

  public LeftoverFood(StockedFood wrapped) {
    super(wrapped);
  }

  @Override
  public Set<FoodStatus> statuses() {
    return withStatus(FoodStatus.LEFTOVER);
  }

  @Override
  public int rescuePriority() {
    return wrapped().rescuePriority() + RESCUE_BONUS;
  }
}
