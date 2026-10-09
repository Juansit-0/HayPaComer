package dev.haypacomer.domain.expiry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.fridge.ZoneKind;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ExpiryRulesTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 10, 9);
  private static final FoodMetadata MILK = food("Milk", FoodCategory.DAIRY, 7);
  private static final FoodMetadata BEEF = food("Ground beef", FoodCategory.MEAT, 2);
  private static final ShelfLife MILK_LIFE = new ShelfLife(7, 5, 90, 4);
  private static final ShelfLife BEEF_LIFE = new ShelfLife(2, 1, 120, 2);
  private final ExpiryRules rules = ExpiryRules.DEFAULT;

  private static FoodMetadata food(String name, FoodCategory category, int days) {
    return new FoodMetadata(
        name, category, Unit.GRAM, ConversionFactors.MASS_ONLY, true, days, Set.of());
  }

  @Test
  void milkOrMeatAMonthAwayIsRejectedWithTheReason() {
    ImpossibleExpiryException milk =
        assertThrows(
            ImpossibleExpiryException.class,
            () ->
                rules.check(
                    MILK,
                    MILK_LIFE,
                    ZoneKind.SHELF,
                    false,
                    TODAY.plusDays(40),
                    ExpirySource.USER,
                    TODAY));
    assertEquals(
        "Milk lasts about 7 days in the fridge, so a date 40 days away is not possible;"
            + " check the label or let HayPaComer estimate it",
        milk.getMessage());
    assertThrows(
        ImpossibleExpiryException.class,
        () ->
            rules.check(
                BEEF,
                BEEF_LIFE,
                ZoneKind.DRAWER,
                false,
                TODAY.plusMonths(1),
                ExpirySource.LABEL,
                TODAY));
    assertTrue(
        assertThrows(
                ImpossibleExpiryException.class,
                () ->
                    rules.check(
                        MILK,
                        MILK_LIFE,
                        ZoneKind.DOOR,
                        false,
                        TODAY.plusDays(9),
                        ExpirySource.USER,
                        TODAY))
            .getMessage()
            .contains("in the fridge door"));
    assertTrue(
        assertThrows(
                ImpossibleExpiryException.class,
                () ->
                    rules.check(
                        MILK,
                        MILK_LIFE,
                        ZoneKind.SHELF,
                        true,
                        TODAY.plusDays(8),
                        ExpirySource.USER,
                        TODAY))
            .getMessage()
            .contains("once opened"));
  }

  @Test
  void datesWithinTheShelfLifeAndTheMarginAreKept() {
    ExpiryEstimate kept =
        rules.check(
            MILK, MILK_LIFE, ZoneKind.SHELF, false, TODAY.plusDays(10), ExpirySource.USER, TODAY);

    assertEquals(TODAY.plusDays(10), kept.date());
    assertEquals(ExpirySource.USER, kept.source());
    assertEquals(1.0, kept.confidence());
    assertEquals(
        TODAY.plusMonths(3),
        rules
            .check(
                BEEF,
                BEEF_LIFE,
                ZoneKind.FREEZER,
                false,
                TODAY.plusMonths(3),
                ExpirySource.USER,
                TODAY)
            .date());
    assertEquals(
        "The expiry date is in the past",
        assertThrows(
                ImpossibleExpiryException.class,
                () ->
                    rules.check(
                        MILK,
                        MILK_LIFE,
                        ZoneKind.SHELF,
                        false,
                        TODAY.minusDays(1),
                        ExpirySource.USER,
                        TODAY))
            .getMessage());
    assertThrows(
        ImpossibleExpiryException.class,
        () ->
            new ExpiryRules(0)
                .check(
                    MILK,
                    MILK_LIFE,
                    ZoneKind.SHELF,
                    false,
                    TODAY.plusDays(8),
                    ExpirySource.USER,
                    TODAY));
    assertTrue(
        assertThrows(
                ImpossibleExpiryException.class,
                () ->
                    rules.check(
                        BEEF,
                        BEEF_LIFE,
                        ZoneKind.FREEZER,
                        false,
                        TODAY.plusYears(1),
                        ExpirySource.USER,
                        TODAY))
            .getMessage()
            .contains("in the freezer"));
    assertThrows(IllegalArgumentException.class, () -> new ExpiryRules(-1));
  }

  @Test
  void unknownDatesAreEstimatedByStorage() {
    ExpiryEstimate fridge = rules.estimate(BEEF_LIFE, ZoneKind.SHELF, false, TODAY);
    assertEquals(TODAY.plusDays(2), fridge.date());
    assertEquals(ExpirySource.ESTIMATED, fridge.source());
    assertEquals(0.6, fridge.confidence());
    assertEquals(
        TODAY.plusDays(120), rules.estimate(BEEF_LIFE, ZoneKind.FREEZER, true, TODAY).date());
    assertEquals(TODAY.plusDays(5), rules.estimate(MILK_LIFE, ZoneKind.DOOR, false, TODAY).date());
    assertEquals(TODAY.plusDays(4), rules.estimate(MILK_LIFE, ZoneKind.SHELF, true, TODAY).date());
  }

  @Test
  void openingOnlyBringsTheDateCloser() {
    assertEquals(
        TODAY.plusDays(4), rules.afterOpening(MILK_LIFE, ZoneKind.SHELF, TODAY.plusDays(7), TODAY));
    assertEquals(
        TODAY.plusDays(2), rules.afterOpening(MILK_LIFE, ZoneKind.SHELF, TODAY.plusDays(2), TODAY));
    assertEquals(TODAY.plusDays(4), rules.afterOpening(MILK_LIFE, ZoneKind.SHELF, null, TODAY));
  }

  @Test
  void shelfLifeFallsBackToTheCatalogDays() {
    ShelfLife fallback = ShelfLife.of(MILK);

    assertEquals(new ShelfLife(7, 7, 90, 7), fallback);
    assertEquals(400, ShelfLife.of(food("Rice", FoodCategory.GRAIN, 400)).freezerDays());
    assertThrows(IllegalArgumentException.class, () -> new ShelfLife(-1, 1, 1, 1));
  }

  @Test
  void foodItemsRememberWhereTheirDateCameFromAndWhenTheyWereOpened() {
    FoodItem typed = new FoodItem(FoodItemId.newId(), MILK, Grams.of(800), Grams.ZERO, TODAY);
    FoodItem none = new FoodItem(FoodItemId.newId(), MILK, Grams.of(800), Grams.ZERO, null);
    FoodItem estimated =
        FoodItem.weighed(
            MILK,
            Grams.of(850),
            Grams.of(50),
            new ExpiryEstimate(TODAY.plusDays(7), ExpirySource.ESTIMATED, 7),
            null);

    assertEquals(ExpirySource.USER, typed.expirySource().orElseThrow());
    assertTrue(none.expirySource().isEmpty());
    assertEquals(ExpirySource.ESTIMATED, estimated.expirySource().orElseThrow());
    assertEquals(Grams.of(800), estimated.quantity());

    estimated.open(TODAY, TODAY.plusDays(4), ExpirySource.ESTIMATED);
    estimated.open(TODAY.plusDays(1), TODAY.plusDays(3), ExpirySource.ESTIMATED);

    assertTrue(estimated.isOpened());
    assertEquals(TODAY, estimated.openedOn().orElseThrow());
    assertEquals(TODAY.plusDays(3), estimated.expiresOn().orElseThrow());
  }
}
