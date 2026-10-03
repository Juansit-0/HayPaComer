package dev.haypacomer.domain.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MarketListTest {

  private static final Instant NOW = Instant.parse("2026-10-03T12:00:00Z");
  private static final FoodMetadata RICE = food("Rice", FoodCategory.GRAIN);
  private static final FoodMetadata CHICKEN = food("Chicken breast", FoodCategory.POULTRY);
  private static final FoodMetadata PASTA = food("Pasta", FoodCategory.GRAIN);

  private final UserId juan = UserId.newId();
  private final UserId ana = UserId.newId();
  private final MarketList list = MarketList.empty(HouseholdId.newId());

  private static FoodMetadata food(String name, FoodCategory category) {
    return new FoodMetadata(
        name, category, Unit.GRAM, ConversionFactors.MASS_ONLY, false, 30, Set.of());
  }

  @Test
  void mergesPendingDuplicatesAcrossMembers() {
    MarketItem first = list.add(RICE, Grams.of(500), MarketSource.MANUAL, juan, NOW);
    MarketItem merged =
        list.add(food("RICE", FoodCategory.GRAIN), Grams.of(250), MarketSource.RECIPE, ana, NOW);

    assertEquals(first.id(), merged.id());
    assertEquals(Grams.of(750), merged.grams());
    assertEquals(juan, merged.addedBy());
    assertEquals(1, list.pending().size());
  }

  @Test
  void checkedItemsDoNotBlockANewPendingEntry() {
    MarketItem bought =
        list.check(list.add(RICE, Grams.of(500), MarketSource.MANUAL, juan, NOW).id(), NOW);
    MarketItem again = list.add(RICE, Grams.of(200), MarketSource.MANUAL, ana, NOW);

    assertFalse(bought.isPending());
    assertTrue(again.isPending());
    assertEquals(2, list.items().size());
    assertThrows(IllegalStateException.class, () -> list.uncheck(bought.id()));
    assertEquals(1, list.clearChecked());
    assertEquals(List.of(again), list.items());
  }

  @Test
  void checkAndUncheckAreIdempotent() {
    MarketItem item = list.add(CHICKEN, Grams.of(400), MarketSource.MANUAL, juan, NOW);

    MarketItem checked = list.check(item.id(), NOW);
    assertSame(checked, list.check(item.id(), NOW.plusSeconds(5)));
    MarketItem pending = list.uncheck(item.id());
    assertTrue(pending.isPending());
    assertSame(pending, list.uncheck(item.id()));
  }

  @Test
  void groupsPendingItemsByCategoryAlphabetically() {
    list.add(RICE, Grams.of(500), MarketSource.MANUAL, juan, NOW);
    list.add(CHICKEN, Grams.of(400), MarketSource.MANUAL, juan, NOW);
    list.add(PASTA, Grams.of(300), MarketSource.MANUAL, juan, NOW);

    Map<FoodCategory, List<MarketItem>> grouped = list.pendingByCategory();

    assertEquals(List.of(FoodCategory.POULTRY, FoodCategory.GRAIN), List.copyOf(grouped.keySet()));
    assertEquals(
        List.of("Pasta", "Rice"),
        grouped.get(FoodCategory.GRAIN).stream().map(i -> i.food().name()).toList());
  }

  @Test
  void editsAndRemovesItems() {
    MarketItem item = list.add(RICE, Grams.of(500), MarketSource.MANUAL, juan, NOW);

    assertEquals(Grams.of(1000), list.changeGrams(item.id(), Grams.of(1000)).grams());
    list.remove(item.id());

    assertTrue(list.items().isEmpty());
    assertThrows(IllegalArgumentException.class, () -> list.remove(item.id()));
    assertThrows(
        IllegalArgumentException.class, () -> list.changeGrams(MarketItemId.newId(), Grams.of(1)));
    assertThrows(
        IllegalArgumentException.class,
        () -> list.add(RICE, Grams.ZERO, MarketSource.MANUAL, juan, NOW));
  }

  @Test
  void restoresExistingItems() {
    MarketItem item = list.add(RICE, Grams.of(500), MarketSource.MANUAL, juan, NOW);

    MarketList restored = MarketList.restore(list.household(), list.items());

    assertEquals(List.of(item), restored.items());
    assertEquals(list.household(), restored.household());
  }
}
