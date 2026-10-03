package dev.haypacomer.domain.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class StockedFoodTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 10, 3);
  private static final FoodMetadata YOGURT =
      new FoodMetadata(
          "Yogurt", FoodCategory.DAIRY, Unit.GRAM, ConversionFactors.MASS_ONLY, true, 10, Set.of());

  private final MemberId juan = MemberId.newId();
  private final MemberId ana = MemberId.newId();

  private static PlainFood plain(String name, LocalDate expiry) {
    FoodMetadata food =
        new FoodMetadata(
            name, FoodCategory.PREPARED, Unit.GRAM, ConversionFactors.MASS_ONLY, true, 3, Set.of());
    return new PlainFood(new FoodItem(FoodItemId.newId(), food, Grams.of(300), Grams.ZERO, expiry));
  }

  private static PlainFood yogurt(LocalDate expiry) {
    return new PlainFood(
        new FoodItem(FoodItemId.newId(), YOGURT, Grams.of(125), Grams.ZERO, expiry));
  }

  @Test
  void plainFoodHasNoStatusAndIsUsableByEveryone() {
    PlainFood food = yogurt(TODAY.plusDays(10));

    assertTrue(food.statuses().isEmpty());
    assertEquals(0, food.rescuePriority());
    assertTrue(food.isEdible());
    assertTrue(food.isUsableBy(ana));
    assertEquals("Yogurt 125 g", food.describe());
  }

  @Test
  void decoratorsStackStatusesAndPriority() {
    StockedFood soup = new LeftoverFood(new AtRiskFood(plain("Soup", TODAY)));

    assertEquals(Set.of(FoodStatus.AT_RISK, FoodStatus.LEFTOVER), soup.statuses());
    assertEquals(3, soup.rescuePriority());
    assertTrue(soup.isEdible());
    assertTrue(soup.has(FoodStatus.LEFTOVER));
    assertTrue(soup.describe().startsWith("Soup 300 g ["));
  }

  @Test
  void expiredFoodIsNotEdible() {
    StockedFood expired = new ExpiredFood(yogurt(TODAY.minusDays(1)));

    assertFalse(expired.isEdible());
    assertTrue(expired.has(FoodStatus.EXPIRED));
    assertEquals(0, expired.rescuePriority());
  }

  @Test
  void privateFoodIsOnlyForItsOwnerUntilGranted() {
    Ownership ownership = Ownership.of(juan, Visibility.PRIVATE);
    OwnedFood yogurt = new OwnedFood(yogurt(TODAY.plusDays(5)), ownership);

    assertTrue(yogurt.isUsableBy(juan));
    assertFalse(yogurt.isUsableBy(ana));
    assertTrue(yogurt.has(FoodStatus.PRIVATE));
    assertFalse(yogurt.needsPermission(ana));

    OwnedFood granted = new OwnedFood(yogurt(TODAY.plusDays(5)), ownership.grant(ana));

    assertTrue(granted.isUsableBy(ana));
    assertFalse(new OwnedFood(granted, ownership.grant(ana).revoke(ana)).isUsableBy(ana));
  }

  @Test
  void askFirstFoodNeedsPermission() {
    OwnedFood cheese = new OwnedFood(yogurt(null), Ownership.of(juan, Visibility.ASK_FIRST));

    assertTrue(cheese.needsPermission(ana));
    assertFalse(cheese.needsPermission(juan));
    assertFalse(cheese.isUsableBy(ana));
    assertTrue(cheese.has(FoodStatus.ASK_FIRST));
    assertEquals(juan, cheese.ownership().owner());
  }

  @Test
  void sharedFoodIsUsableByEveryone() {
    OwnedFood rice = new OwnedFood(yogurt(null), Ownership.of(juan, Visibility.SHARED));

    assertTrue(rice.isUsableBy(ana));
    assertTrue(rice.statuses().isEmpty());
  }

  @Test
  void ownershipCombinesWithOtherDecorators() {
    StockedFood food =
        new AtRiskFood(new OwnedFood(yogurt(TODAY), Ownership.of(juan, Visibility.PRIVATE)));

    assertFalse(food.isUsableBy(ana));
    assertEquals(Set.of(FoodStatus.PRIVATE, FoodStatus.AT_RISK), food.statuses());
  }

  @Test
  void freshnessPolicyAppliesTheRightDecorator() {
    FreshnessPolicy policy = FreshnessPolicy.DEFAULT;

    assertInstanceOf(ExpiredFood.class, policy.apply(yogurt(TODAY.minusDays(1)), TODAY));
    assertInstanceOf(AtRiskFood.class, policy.apply(yogurt(TODAY), TODAY));
    assertInstanceOf(AtRiskFood.class, policy.apply(yogurt(TODAY.plusDays(2)), TODAY));
    StockedFood fresh = yogurt(TODAY.plusDays(3));
    assertSame(fresh, policy.apply(fresh, TODAY));
    StockedFood noExpiry = yogurt(null);
    assertSame(noExpiry, policy.apply(noExpiry, TODAY));
  }

  @Test
  void rescueOrderPutsEdibleHighPriorityAndSoonestFirst() {
    StockedFood expired = new ExpiredFood(plain("Old milk", TODAY.minusDays(2)));
    StockedFood fresh = plain("Carrots", TODAY.plusDays(9));
    StockedFood atRisk = new AtRiskFood(plain("Chicken", TODAY.plusDays(1)));
    StockedFood leftoverAtRisk = new LeftoverFood(new AtRiskFood(plain("Soup", TODAY)));
    StockedFood noExpiry = plain("Rice", null);

    List<String> order =
        Stream.of(expired, fresh, noExpiry, atRisk, leftoverAtRisk)
            .sorted(StockedFood.RESCUE_ORDER)
            .map(food -> food.item().name())
            .toList();

    assertEquals(List.of("Soup", "Chicken", "Carrots", "Rice", "Old milk"), order);
  }

  @Test
  void rejectsInvalidInputs() {
    assertThrows(NullPointerException.class, () -> new PlainFood(null));
    assertThrows(NullPointerException.class, () -> new ExpiredFood(null));
    assertThrows(NullPointerException.class, () -> new OwnedFood(yogurt(null), null));
    assertThrows(NullPointerException.class, () -> Ownership.of(null, Visibility.SHARED));
    assertThrows(IllegalArgumentException.class, () -> new FreshnessPolicy(-1));
    assertThrows(
        NullPointerException.class, () -> FreshnessPolicy.DEFAULT.apply(yogurt(null), null));
  }
}
