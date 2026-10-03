package dev.haypacomer.domain.fridge;

import dev.haypacomer.domain.quantity.Grams;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public sealed interface FridgeNode extends Iterable<FridgeNode>
    permits Fridge, Zone, Tray, FoodItem {

  String name();

  Grams totalGrams();

  int itemCount();

  List<? extends FridgeNode> children();

  @Override
  default Iterator<FridgeNode> iterator() {
    return iterator(Traversal.DEPTH_FIRST);
  }

  default Iterator<FridgeNode> iterator(Traversal traversal) {
    return new FridgeTreeIterator(this, traversal);
  }

  default Stream<FridgeNode> nodes(Traversal traversal) {
    return StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(
            iterator(traversal), Spliterator.ORDERED | Spliterator.NONNULL),
        false);
  }

  default Stream<FoodItem> foodItems() {
    return nodes(Traversal.DEPTH_FIRST)
        .filter(FoodItem.class::isInstance)
        .map(FoodItem.class::cast);
  }

  default Stream<Tray> trays() {
    return nodes(Traversal.DEPTH_FIRST).filter(Tray.class::isInstance).map(Tray.class::cast);
  }
}
