package dev.haypacomer.domain.inventory;

import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.member.MemberId;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class PrivateFoodProxy implements StockedFood {

  public static final FoodMetadata HIDDEN =
      new FoodMetadata(
          "Private food",
          FoodCategory.OTHER,
          Unit.GRAM,
          ConversionFactors.MASS_ONLY,
          false,
          0,
          Set.of());

  private final StockedFood real;
  private final FoodItem mask;

  private PrivateFoodProxy(StockedFood real) {
    this.real = real;
    this.mask = new FoodItem(real.item().id(), HIDDEN, Grams.ZERO, Grams.ZERO, null);
  }

  public static StockedFood guard(StockedFood food, MemberId viewer) {
    Objects.requireNonNull(food, "food");
    Objects.requireNonNull(viewer, "viewer");
    if (food.has(FoodStatus.PRIVATE) && !food.isUsableBy(viewer)) {
      return new PrivateFoodProxy(food);
    }
    return food;
  }

  public Optional<StockedFood> revealTo(MemberId member) {
    return real.isUsableBy(member) ? Optional.of(real) : Optional.empty();
  }

  @Override
  public FoodItem item() {
    return mask;
  }

  @Override
  public Set<FoodStatus> statuses() {
    return Set.of(FoodStatus.PRIVATE);
  }

  @Override
  public int rescuePriority() {
    return 0;
  }

  @Override
  public boolean isEdible() {
    return false;
  }

  @Override
  public boolean isUsableBy(MemberId member) {
    return real.isUsableBy(member);
  }
}
