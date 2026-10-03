package dev.haypacomer.domain.fridge;

import dev.haypacomer.domain.quantity.Grams;
import java.util.List;

public sealed interface FridgeNode permits Fridge, Zone, Tray, FoodItem {

  String name();

  Grams totalGrams();

  int itemCount();

  List<? extends FridgeNode> children();
}
