package dev.haypacomer.domain.fridge;

import static dev.haypacomer.domain.fridge.FridgeFixtures.CHICKEN;
import static dev.haypacomer.domain.fridge.FridgeFixtures.MILK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FridgeTreeIteratorTest {

  private Fridge fridge;
  private Zone shelves;
  private Zone door;
  private Tray top;
  private Tray bottom;
  private Tray rack;
  private FoodItem chicken;
  private FoodItem leftovers;
  private FoodItem milk;

  @BeforeEach
  void buildTree() {
    fridge = Fridge.named("Kitchen fridge");
    shelves = Zone.named("Shelves", ZoneKind.SHELF);
    door = Zone.named("Door", ZoneKind.DOOR);
    top = Tray.named("Top", 0);
    bottom = Tray.named("Bottom", 1);
    rack = Tray.named("Rack", 0);
    shelves.add(top);
    shelves.add(bottom);
    door.add(rack);
    fridge.add(shelves);
    fridge.add(door);
    chicken = FridgeFixtures.item(CHICKEN, 80);
    leftovers = FridgeFixtures.item(CHICKEN, 120);
    milk = FridgeFixtures.item(MILK, 842);
    fridge.place(chicken, top.id());
    fridge.place(leftovers, top.id());
    fridge.place(milk, rack.id());
  }

  @Test
  void depthFirstVisitsEachBranchBeforeTheNext() {
    List<FridgeNode> visited = new ArrayList<>();
    fridge.iterator(Traversal.DEPTH_FIRST).forEachRemaining(visited::add);

    assertEquals(
        List.of(fridge, shelves, top, chicken, leftovers, bottom, door, rack, milk), visited);
  }

  @Test
  void breadthFirstVisitsLevelByLevel() {
    List<FridgeNode> visited = fridge.nodes(Traversal.BREADTH_FIRST).toList();

    assertEquals(
        List.of(fridge, shelves, door, top, bottom, rack, chicken, leftovers, milk), visited);
  }

  @Test
  void worksWithEnhancedForLoopFromAnyNode() {
    List<FridgeNode> visited = new ArrayList<>();
    for (FridgeNode node : shelves) {
      visited.add(node);
    }

    assertEquals(List.of(shelves, top, chicken, leftovers, bottom), visited);
  }

  @Test
  void leafYieldsOnlyItself() {
    assertEquals(List.of(milk), milk.nodes(Traversal.DEPTH_FIRST).toList());
  }

  @Test
  void streamsFoodItemsAndTrays() {
    assertEquals(List.of(chicken, leftovers, milk), fridge.foodItems().toList());
    assertEquals(List.of(top, bottom, rack), fridge.trays().toList());
    assertEquals(List.of(milk), door.foodItems().toList());
  }

  @Test
  void failsAfterTheLastNode() {
    Iterator<FridgeNode> iterator = milk.iterator();
    iterator.next();

    assertFalse(iterator.hasNext());
    assertThrows(NoSuchElementException.class, iterator::next);
  }

  @Test
  void requiresRootAndTraversal() {
    assertThrows(
        NullPointerException.class, () -> new FridgeTreeIterator(null, Traversal.DEPTH_FIRST));
    assertThrows(NullPointerException.class, () -> new FridgeTreeIterator(fridge, null));
  }
}
