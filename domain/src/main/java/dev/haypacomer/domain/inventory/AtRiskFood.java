package dev.haypacomer.domain.inventory;

import java.util.Set;

public final class AtRiskFood extends StockedFoodDecorator {

  private static final int RESCUE_BONUS = 2;

  public AtRiskFood(StockedFood wrapped) {
    super(wrapped);
  }

  @Override
  public Set<FoodStatus> statuses() {
    return withStatus(FoodStatus.AT_RISK);
  }

  @Override
  public int rescuePriority() {
    return wrapped().rescuePriority() + RESCUE_BONUS;
  }
}
