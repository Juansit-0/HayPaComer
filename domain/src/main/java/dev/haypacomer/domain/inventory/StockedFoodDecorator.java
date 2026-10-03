package dev.haypacomer.domain.inventory;

import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.member.MemberId;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public abstract class StockedFoodDecorator implements StockedFood {

  private final StockedFood wrapped;

  protected StockedFoodDecorator(StockedFood wrapped) {
    this.wrapped = Objects.requireNonNull(wrapped, "wrapped");
  }

  protected StockedFood wrapped() {
    return wrapped;
  }

  protected Set<FoodStatus> withStatus(FoodStatus status) {
    Set<FoodStatus> statuses = EnumSet.noneOf(FoodStatus.class);
    statuses.addAll(wrapped.statuses());
    statuses.add(status);
    return Set.copyOf(statuses);
  }

  @Override
  public FoodItem item() {
    return wrapped.item();
  }

  @Override
  public Set<FoodStatus> statuses() {
    return wrapped.statuses();
  }

  @Override
  public int rescuePriority() {
    return wrapped.rescuePriority();
  }

  @Override
  public boolean isEdible() {
    return wrapped.isEdible();
  }

  @Override
  public boolean isUsableBy(MemberId member) {
    return wrapped.isUsableBy(member);
  }
}
