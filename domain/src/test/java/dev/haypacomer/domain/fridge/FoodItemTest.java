package dev.haypacomer.domain.fridge;

import static dev.haypacomer.domain.fridge.FridgeFixtures.CHICKEN;
import static dev.haypacomer.domain.fridge.FridgeFixtures.MILK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.quantity.Grams;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class FoodItemTest {

  @Test
  void weighedItemDiscountsTare() {
    FoodItem milk = FoodItem.weighed(MILK, Grams.of(892), Grams.of(50), LocalDate.of(2026, 10, 5));

    assertEquals(Grams.of(842), milk.quantity());
    assertEquals(Grams.of(892), milk.grossWeight());
    assertEquals(Grams.of(50), milk.tare());
    assertEquals(LocalDate.of(2026, 10, 5), milk.expiresOn().orElseThrow());
  }

  @Test
  void consumesAndRestocksMeasuredGrams() {
    FoodItem milk = FridgeFixtures.item(MILK, 842);

    assertEquals(Grams.of(650), milk.consume(Grams.of(192)));
    assertEquals(Grams.of(700), milk.restock(Grams.of(50)));
  }

  @Test
  void cannotConsumeMoreThanMeasured() {
    FoodItem chicken = FridgeFixtures.item(CHICKEN, 80);

    assertThrows(IllegalArgumentException.class, () -> chicken.consume(Grams.of(200)));
    assertEquals(Grams.of(80), chicken.quantity());
  }

  @Test
  void becomesEmptyWhenFullyConsumed() {
    FoodItem chicken = FridgeFixtures.item(CHICKEN, 80);

    chicken.consume(Grams.of(80));

    assertTrue(chicken.isEmpty());
  }

  @Test
  void isALeafOfTheTree() {
    FoodItem milk = FridgeFixtures.item(MILK, 842);

    assertEquals("Milk", milk.name());
    assertSame(MILK, milk.food());
    assertEquals(1, milk.itemCount());
    assertEquals(Grams.of(842), milk.totalGrams());
    assertTrue(milk.children().isEmpty());
  }

  @Test
  void expiryIsOptional() {
    FoodItem rice = new FoodItem(FoodItemId.newId(), CHICKEN, Grams.of(500), Grams.ZERO, null);

    assertTrue(rice.expiresOn().isEmpty());
  }

  @Test
  void requiresQuantity() {
    assertThrows(
        NullPointerException.class,
        () -> new FoodItem(FoodItemId.newId(), MILK, null, Grams.ZERO, null));
  }
}
