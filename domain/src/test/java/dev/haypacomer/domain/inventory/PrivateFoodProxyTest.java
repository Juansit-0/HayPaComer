package dev.haypacomer.domain.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.member.MemberId;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PrivateFoodProxyTest {

  private final MemberId juan = MemberId.newId();
  private final MemberId ana = MemberId.newId();
  private final FoodItem cake =
      new FoodItem(
          FoodItemId.newId(),
          new FoodMetadata(
              "Birthday cake",
              FoodCategory.PREPARED,
              Unit.GRAM,
              ConversionFactors.MASS_ONLY,
              true,
              3,
              Set.of()),
          Grams.of(900),
          Grams.ZERO,
          LocalDate.of(2026, 10, 10));

  private StockedFood owned(Visibility visibility) {
    return new OwnedFood(new PlainFood(cake), Ownership.of(juan, visibility));
  }

  @Test
  void hidesPrivateFoodFromEveryoneButTheOwner() {
    StockedFood forAna = PrivateFoodProxy.guard(owned(Visibility.PRIVATE), ana);

    PrivateFoodProxy proxy = assertInstanceOf(PrivateFoodProxy.class, forAna);
    assertEquals("Private food", proxy.item().name());
    assertEquals(Grams.ZERO, proxy.item().quantity());
    assertTrue(proxy.item().expiresOn().isEmpty());
    assertEquals(cake.id(), proxy.item().id());
    assertEquals(Set.of(FoodStatus.PRIVATE), proxy.statuses());
    assertFalse(proxy.isEdible());
    assertEquals(0, proxy.rescuePriority());
    assertFalse(proxy.isUsableBy(ana));
    assertTrue(proxy.revealTo(ana).isEmpty());
    assertEquals("Birthday cake", proxy.revealTo(juan).orElseThrow().item().name());
  }

  @Test
  void leavesVisibleFoodUntouched() {
    StockedFood mine = owned(Visibility.PRIVATE);
    StockedFood shared = owned(Visibility.SHARED);
    StockedFood askFirst = owned(Visibility.ASK_FIRST);

    assertSame(mine, PrivateFoodProxy.guard(mine, juan));
    assertSame(shared, PrivateFoodProxy.guard(shared, ana));
    assertSame(askFirst, PrivateFoodProxy.guard(askFirst, ana));
  }
}
