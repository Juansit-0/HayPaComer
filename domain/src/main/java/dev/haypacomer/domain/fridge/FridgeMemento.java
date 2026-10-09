package dev.haypacomer.domain.fridge;

import dev.haypacomer.domain.expiry.ExpirySource;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.quantity.Grams;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public record FridgeMemento(FridgeId id, String name, List<ZoneState> zones) {

  public FridgeMemento {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
    zones = List.copyOf(zones);
  }

  public Fridge restore(Function<String, FoodMetadata> foods) {
    Fridge fridge = new Fridge(id, name);
    for (ZoneState zoneState : zones) {
      Zone zone = new Zone(zoneState.id(), zoneState.name(), zoneState.kind());
      for (TrayState trayState : zoneState.trays()) {
        Tray tray = new Tray(trayState.id(), trayState.name(), trayState.position());
        for (ItemState itemState : trayState.items()) {
          tray.add(
              new FoodItem(
                  itemState.id(),
                  Objects.requireNonNull(
                      foods.apply(itemState.foodName()), "Unknown food " + itemState.foodName()),
                  itemState.quantity(),
                  itemState.tare(),
                  itemState.expiresOn(),
                  itemState.expirySource(),
                  itemState.openedOn()));
        }
        zone.add(tray);
      }
      fridge.add(zone);
    }
    return fridge;
  }

  public record ZoneState(ZoneId id, String name, ZoneKind kind, List<TrayState> trays) {

    public ZoneState {
      trays = List.copyOf(trays);
    }
  }

  public record TrayState(TrayId id, String name, int position, List<ItemState> items) {

    public TrayState {
      items = List.copyOf(items);
    }
  }

  public record ItemState(
      FoodItemId id,
      String foodName,
      Grams quantity,
      Grams tare,
      LocalDate expiresOn,
      ExpirySource expirySource,
      LocalDate openedOn) {

    public ItemState(
        FoodItemId id, String foodName, Grams quantity, Grams tare, LocalDate expiresOn) {
      this(id, foodName, quantity, tare, expiresOn, null, null);
    }
  }
}
