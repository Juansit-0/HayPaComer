package dev.haypacomer.domain.inventory;

import java.util.Set;

public final class UnderReviewFood extends StockedFoodDecorator {

  public UnderReviewFood(StockedFood wrapped) {
    super(wrapped);
  }

  @Override
  public Set<FoodStatus> statuses() {
    return withStatus(FoodStatus.UNDER_REVIEW);
  }

  @Override
  public boolean isEdible() {
    return false;
  }
}
