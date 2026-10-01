package dev.haypacomer.domain.fridge;

import static dev.haypacomer.domain.fridge.FridgeFixtures.CHICKEN;
import static dev.haypacomer.domain.fridge.FridgeFixtures.MILK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.quantity.Grams;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FridgeTest {

  private Fridge fridge;
  private Zone shelves;
  private Zone door;
  private Tray top;
  private Tray bottom;
  private Tray doorRack;

  @BeforeEach
  void buildTree() {
    fridge = Fridge.named("Kitchen fridge");
    shelves = Zone.named("Shelves", ZoneKind.SHELF);
    door = Zone.named("Door", ZoneKind.DOOR);
    top = Tray.named("Top", 0);
    bottom = Tray.named("Bottom", 1);
    doorRack = Tray.named("Rack", 0);
    shelves.add(top);
    shelves.add(bottom);
    door.add(doorRack);
    fridge.add(shelves);
    fridge.add(door);
  }

  @Test
  void aggregatesGramsAndItemsAcrossTheTree() {
    fridge.place(FridgeFixtures.item(MILK, 842), doorRack.id());
    fridge.place(FridgeFixtures.item(CHICKEN, 80), top.id());
    fridge.place(FridgeFixtures.item(CHICKEN, 120), bottom.id());

    assertEquals(Grams.of(1042), fridge.totalGrams());
    assertEquals(3, fridge.itemCount());
    assertEquals(Grams.of(200), shelves.totalGrams());
    assertEquals(2, shelves.itemCount());
    assertEquals(Grams.of(842), door.totalGrams());
  }

  @Test
  void treatsEveryNodeUniformly() {
    fridge.place(FridgeFixtures.item(MILK, 842), doorRack.id());
    List<FridgeNode> nodes = List.of(fridge, door, doorRack, doorRack.children().getFirst());

    nodes.forEach(node -> assertEquals(Grams.of(842), node.totalGrams()));
    nodes.forEach(node -> assertEquals(1, node.itemCount()));
  }

  @Test
  void emptyFridgeHasNothing() {
    Fridge empty = Fridge.named("Garage fridge");

    assertEquals(Grams.ZERO, empty.totalGrams());
    assertEquals(0, empty.itemCount());
    assertTrue(empty.children().isEmpty());
  }

  @Test
  void findsItemAndItsTray() {
    FoodItem milk = FridgeFixtures.item(MILK, 842);
    fridge.place(milk, doorRack.id());

    assertSame(milk, fridge.findItem(milk.id()).orElseThrow());
    assertSame(doorRack, fridge.locate(milk.id()).orElseThrow());
    assertTrue(fridge.findItem(FoodItemId.newId()).isEmpty());
  }

  @Test
  void movesItemBetweenTrays() {
    FoodItem chicken = FridgeFixtures.item(CHICKEN, 80);
    fridge.place(chicken, top.id());

    fridge.move(chicken.id(), bottom.id());

    assertSame(bottom, fridge.locate(chicken.id()).orElseThrow());
    assertEquals(0, top.itemCount());
    assertEquals(1, fridge.itemCount());
  }

  @Test
  void moveToSameTrayKeepsItem() {
    FoodItem chicken = FridgeFixtures.item(CHICKEN, 80);
    fridge.place(chicken, top.id());

    fridge.move(chicken.id(), top.id());

    assertSame(top, fridge.locate(chicken.id()).orElseThrow());
  }

  @Test
  void takesItemOutOfTheFridge() {
    FoodItem milk = FridgeFixtures.item(MILK, 842);
    fridge.place(milk, doorRack.id());

    assertSame(milk, fridge.take(milk.id()));
    assertEquals(0, fridge.itemCount());
  }

  @Test
  void rejectsPlacingTheSameItemTwice() {
    FoodItem milk = FridgeFixtures.item(MILK, 842);
    fridge.place(milk, doorRack.id());

    assertThrows(IllegalArgumentException.class, () -> fridge.place(milk, top.id()));
  }

  @Test
  void rejectsUnknownTrayAndItem() {
    FoodItem milk = FridgeFixtures.item(MILK, 842);
    TrayId unknownTray = new TrayId(UUID.randomUUID());

    assertThrows(IllegalArgumentException.class, () -> fridge.place(milk, unknownTray));
    assertThrows(IllegalArgumentException.class, () -> fridge.move(milk.id(), top.id()));
    assertThrows(IllegalArgumentException.class, () -> fridge.take(milk.id()));
  }

  @Test
  void rejectsDuplicateZone() {
    assertThrows(IllegalArgumentException.class, () -> fridge.add(shelves));
  }

  @Test
  void exposesIdentityAndReadOnlyChildren() {
    assertEquals("Kitchen fridge", fridge.name());
    assertEquals(List.of(shelves, door), fridge.children());
    assertThrows(UnsupportedOperationException.class, () -> fridge.children().clear());
    assertTrue(fridge.id().value() != null);
  }
}
