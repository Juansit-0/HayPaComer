package dev.haypacomer.domain.inventory;

import dev.haypacomer.domain.member.MemberId;
import java.util.Objects;
import java.util.Set;

public final class OwnedFood extends StockedFoodDecorator {

  private final Ownership ownership;

  public OwnedFood(StockedFood wrapped, Ownership ownership) {
    super(wrapped);
    this.ownership = Objects.requireNonNull(ownership, "ownership");
  }

  public Ownership ownership() {
    return ownership;
  }

  public boolean needsPermission(MemberId member) {
    return ownership.visibility() == Visibility.ASK_FIRST && !ownership.allows(member);
  }

  @Override
  public Set<FoodStatus> statuses() {
    return switch (ownership.visibility()) {
      case SHARED -> super.statuses();
      case ASK_FIRST -> withStatus(FoodStatus.ASK_FIRST);
      case PRIVATE -> withStatus(FoodStatus.PRIVATE);
    };
  }

  @Override
  public boolean isUsableBy(MemberId member) {
    return ownership.allows(member) && super.isUsableBy(member);
  }
}
