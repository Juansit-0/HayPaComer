package dev.haypacomer.domain.fridge;

import static dev.haypacomer.domain.fridge.FridgeFixtures.MILK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ZoneAndTrayTest {

  @Test
  void zoneKeepsKindAndRejectsDuplicateTrays() {
    Zone drawer = Zone.named("  Vegetables ", ZoneKind.DRAWER);
    Tray tray = Tray.named("Left", 0);
    drawer.add(tray);

    assertEquals("Vegetables", drawer.name());
    assertEquals(ZoneKind.DRAWER, drawer.kind());
    assertEquals(tray, drawer.find(tray.id()).orElseThrow());
    assertThrows(IllegalArgumentException.class, () -> drawer.add(tray));
    assertTrue(drawer.find(TrayId.newId()).isEmpty());
    assertNotEquals(drawer.id(), Zone.named("Other", ZoneKind.SHELF).id());
  }

  @Test
  void trayAddsAndRemovesItems() {
    Tray tray = Tray.named("Top", 2);
    FoodItem milk = FridgeFixtures.item(MILK, 842);
    tray.add(milk);

    assertEquals(2, tray.position());
    assertThrows(IllegalArgumentException.class, () -> tray.add(milk));
    assertEquals(milk, tray.remove(milk.id()));
    assertThrows(IllegalArgumentException.class, () -> tray.remove(milk.id()));
  }

  @Test
  void rejectsInvalidNamesAndPositions() {
    assertThrows(IllegalArgumentException.class, () -> Fridge.named(" "));
    assertThrows(IllegalArgumentException.class, () -> Zone.named("", ZoneKind.SHELF));
    assertThrows(NullPointerException.class, () -> Zone.named(null, ZoneKind.SHELF));
    assertThrows(NullPointerException.class, () -> Zone.named("Door", null));
    assertThrows(IllegalArgumentException.class, () -> Tray.named("Top", -1));
  }

  @Test
  void idsRequireValue() {
    assertThrows(NullPointerException.class, () -> new FridgeId(null));
    assertThrows(NullPointerException.class, () -> new ZoneId(null));
    assertThrows(NullPointerException.class, () -> new TrayId(null));
    assertThrows(NullPointerException.class, () -> new FoodItemId(null));
  }
}
