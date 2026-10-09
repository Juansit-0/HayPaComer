package dev.haypacomer.agent.kitchen;

import dev.haypacomer.application.inventory.InventoryEntry;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.inventory.StockedFood;
import java.util.List;
import java.util.stream.Collectors;

final class InventoryLines {

  static final int LIMIT = 40;

  private InventoryLines() {}

  static String describe(List<InventoryEntry> entries, String empty) {
    if (entries.isEmpty()) {
      return empty;
    }
    String lines =
        entries.stream().limit(LIMIT).map(InventoryLines::line).collect(Collectors.joining("\n"));
    return entries.size() > LIMIT ? lines + "\n... " + (entries.size() - LIMIT) + " more" : lines;
  }

  private static String line(InventoryEntry entry) {
    StockedFood food = entry.food();
    if (!entry.usable()) {
      return "Private food (not yours to use)";
    }
    FoodItem item = food.item();
    StringBuilder line = new StringBuilder(item.name()).append(' ').append(item.quantity());
    item.expiresOn().ifPresent(date -> line.append(", expires ").append(date));
    if (!food.statuses().isEmpty()) {
      line.append(", ").append(food.statuses());
    }
    if (!food.isEdible()) {
      line.append(", not edible");
    }
    return line.toString();
  }
}
