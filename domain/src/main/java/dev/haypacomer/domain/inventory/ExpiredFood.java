package dev.haypacomer.domain.inventory;

import java.util.Set;

public final class ExpiredFood extends StockedFoodDecorator {

  public ExpiredFood(StockedFood wrapped) {
    super(wrapped);
  }

  @Override
  public Set<FoodStatus> statuses() {
    return withStatus(FoodStatus.EXPIRED);
  }

  @Override
  public boolean isEdible() {
    return false;
  }
}
