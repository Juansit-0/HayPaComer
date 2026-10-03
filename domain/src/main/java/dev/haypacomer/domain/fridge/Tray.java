package dev.haypacomer.domain.fridge;

import dev.haypacomer.domain.quantity.Grams;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class Tray implements FridgeNode {

  private final TrayId id;
  private final String name;
  private final int position;
  private final List<FoodItem> items = new ArrayList<>();

  public Tray(TrayId id, String name, int position) {
    this.id = Objects.requireNonNull(id, "id");
    this.name = Names.require(name, "Tray name");
    if (position < 0) {
      throw new IllegalArgumentException("Tray position cannot be negative: " + position);
    }
    this.position = position;
  }

  public static Tray named(String name, int position) {
    return new Tray(TrayId.newId(), name, position);
  }

  public TrayId id() {
    return id;
  }

  public int position() {
    return position;
  }

  public void add(FoodItem item) {
    Objects.requireNonNull(item, "item");
    if (find(item.id()).isPresent()) {
      throw new IllegalArgumentException("Item already in tray " + name + ": " + item.id());
    }
    items.add(item);
  }

  public Optional<FoodItem> find(FoodItemId itemId) {
    return items.stream().filter(item -> item.id().equals(itemId)).findFirst();
  }

  public FoodItem remove(FoodItemId itemId) {
    FoodItem item =
        find(itemId)
            .orElseThrow(
                () -> new IllegalArgumentException("Item not in tray " + name + ": " + itemId));
    items.remove(item);
    return item;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Grams totalGrams() {
    return items.stream().map(FoodItem::totalGrams).reduce(Grams.ZERO, Grams::plus);
  }

  @Override
  public int itemCount() {
    return items.size();
  }

  @Override
  public List<FoodItem> children() {
    return List.copyOf(items);
  }
}
