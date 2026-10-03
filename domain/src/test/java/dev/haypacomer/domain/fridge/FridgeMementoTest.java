package dev.haypacomer.domain.fridge;

import static dev.haypacomer.domain.fridge.FridgeFixtures.CHICKEN;
import static dev.haypacomer.domain.fridge.FridgeFixtures.MILK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.quantity.Grams;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class FridgeMementoTest {

  private static final Function<String, FoodMetadata> FOODS =
      Map.of(MILK.name(), MILK, CHICKEN.name(), CHICKEN)::get;

  @Test
  void restoresTheExactTreeAfterLaterChanges() {
    Fridge fridge = Fridge.named("Kitchen");
    Zone door = Zone.named("Door", ZoneKind.DOOR);
    Tray rack = Tray.named("Rack", 0);
    Tray low = Tray.named("Low", 1);
    door.add(rack);
    door.add(low);
    fridge.add(door);
    FoodItem milk = FridgeFixtures.item(MILK, 842);
    FoodItem chicken = FridgeFixtures.item(CHICKEN, 80);
    fridge.place(milk, rack.id());
    fridge.place(chicken, low.id());

    FridgeMemento memento = fridge.snapshot();
    milk.consume(Grams.of(192));
    fridge.take(chicken.id());

    Fridge restored = memento.restore(FOODS);

    assertNotSame(fridge, restored);
    assertEquals(fridge.id(), restored.id());
    assertEquals(Grams.of(922), restored.totalGrams());
    assertEquals(Grams.of(842), restored.findItem(milk.id()).orElseThrow().quantity());
    assertEquals(low.id(), restored.locate(chicken.id()).orElseThrow().id());
    assertEquals(memento, restored.snapshot());
  }

  @Test
  void mementoIsImmutableAndNeedsKnownFoods() {
    Fridge fridge = Fridge.named("Kitchen");
    Zone shelves = Zone.named("Shelves", ZoneKind.SHELF);
    Tray top = Tray.named("Top", 0);
    shelves.add(top);
    fridge.add(shelves);
    fridge.place(FridgeFixtures.item(MILK, 842), top.id());
    FridgeMemento memento = fridge.snapshot();

    assertThrows(UnsupportedOperationException.class, () -> memento.zones().clear());
    assertThrows(NullPointerException.class, () -> memento.restore(name -> null));
    assertTrue(Fridge.named("Empty").snapshot().zones().isEmpty());
  }
}
