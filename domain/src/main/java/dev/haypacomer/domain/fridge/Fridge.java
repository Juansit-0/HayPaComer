package dev.haypacomer.domain.fridge;

import dev.haypacomer.domain.quantity.Grams;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public final class Fridge implements FridgeNode {

  private final FridgeId id;
  private final String name;
  private final List<Zone> zones = new ArrayList<>();

  public Fridge(FridgeId id, String name) {
    this.id = Objects.requireNonNull(id, "id");
    this.name = Names.require(name, "Fridge name");
  }

  public static Fridge named(String name) {
    return new Fridge(FridgeId.newId(), name);
  }

  public FridgeId id() {
    return id;
  }

  public void add(Zone zone) {
    Objects.requireNonNull(zone, "zone");
    if (zones.stream().anyMatch(existing -> existing.id().equals(zone.id()))) {
      throw new IllegalArgumentException("Zone already in fridge " + name + ": " + zone.id());
    }
    zones.add(zone);
  }

  public Optional<Tray> findTray(TrayId trayId) {
    return zones.stream().flatMap(zone -> zone.find(trayId).stream()).findFirst();
  }

  public Optional<Tray> locate(FoodItemId itemId) {
    return trays().filter(tray -> tray.find(itemId).isPresent()).findFirst();
  }

  public Optional<FoodItem> findItem(FoodItemId itemId) {
    return trays().flatMap(tray -> tray.find(itemId).stream()).findFirst();
  }

  public void place(FoodItem item, TrayId trayId) {
    Objects.requireNonNull(item, "item");
    if (locate(item.id()).isPresent()) {
      throw new IllegalArgumentException("Item already in fridge " + name + ": " + item.id());
    }
    requireTray(trayId).add(item);
  }

  public void move(FoodItemId itemId, TrayId targetTrayId) {
    Tray target = requireTray(targetTrayId);
    Tray source =
        locate(itemId)
            .orElseThrow(
                () -> new IllegalArgumentException("Item not in fridge " + name + ": " + itemId));
    if (source != target) {
      target.add(source.remove(itemId));
    }
  }

  public FoodItem take(FoodItemId itemId) {
    return locate(itemId)
        .orElseThrow(
            () -> new IllegalArgumentException("Item not in fridge " + name + ": " + itemId))
        .remove(itemId);
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Grams totalGrams() {
    return zones.stream().map(Zone::totalGrams).reduce(Grams.ZERO, Grams::plus);
  }

  @Override
  public int itemCount() {
    return zones.stream().mapToInt(Zone::itemCount).sum();
  }

  @Override
  public List<Zone> children() {
    return List.copyOf(zones);
  }

  private Tray requireTray(TrayId trayId) {
    return findTray(trayId)
        .orElseThrow(
            () -> new IllegalArgumentException("Tray not in fridge " + name + ": " + trayId));
  }

  private Stream<Tray> trays() {
    return zones.stream().flatMap(zone -> zone.children().stream());
  }
}
