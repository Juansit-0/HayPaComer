package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.inventory.InventoryMemento;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.fridge.FridgeMemento;
import dev.haypacomer.domain.fridge.TrayId;
import dev.haypacomer.domain.fridge.ZoneId;
import dev.haypacomer.domain.fridge.ZoneKind;
import dev.haypacomer.domain.inventory.Ownership;
import dev.haypacomer.domain.inventory.Visibility;
import dev.haypacomer.domain.member.MemberId;
import dev.haypacomer.domain.quantity.Grams;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import tools.jackson.databind.json.JsonMapper;

record SnapshotPayload(List<FridgeJson> fridges, List<OwnershipJson> ownerships) {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  static String write(InventoryMemento memento) {
    return JSON.writeValueAsString(from(memento));
  }

  static InventoryMemento read(String json) {
    return JSON.readValue(json, SnapshotPayload.class).toMemento();
  }

  private static SnapshotPayload from(InventoryMemento memento) {
    return new SnapshotPayload(
        memento.fridges().stream().map(FridgeJson::from).toList(),
        memento.ownerships().entrySet().stream()
            .map(entry -> OwnershipJson.from(entry.getKey(), entry.getValue()))
            .toList());
  }

  private InventoryMemento toMemento() {
    Map<FoodItemId, Ownership> owned = new HashMap<>();
    ownerships.forEach(
        ownership -> owned.put(new FoodItemId(ownership.item()), ownership.toOwnership()));
    return new InventoryMemento(fridges.stream().map(FridgeJson::toMemento).toList(), owned);
  }

  record FridgeJson(UUID id, String name, List<ZoneJson> zones) {

    static FridgeJson from(FridgeMemento fridge) {
      return new FridgeJson(
          fridge.id().value(), fridge.name(), fridge.zones().stream().map(ZoneJson::from).toList());
    }

    FridgeMemento toMemento() {
      return new FridgeMemento(
          new FridgeId(id), name, zones.stream().map(ZoneJson::toState).toList());
    }
  }

  record ZoneJson(UUID id, String name, ZoneKind kind, List<TrayJson> trays) {

    static ZoneJson from(FridgeMemento.ZoneState zone) {
      return new ZoneJson(
          zone.id().value(),
          zone.name(),
          zone.kind(),
          zone.trays().stream().map(TrayJson::from).toList());
    }

    FridgeMemento.ZoneState toState() {
      return new FridgeMemento.ZoneState(
          new ZoneId(id), name, kind, trays.stream().map(TrayJson::toState).toList());
    }
  }

  record TrayJson(UUID id, String name, int position, List<ItemJson> items) {

    static TrayJson from(FridgeMemento.TrayState tray) {
      return new TrayJson(
          tray.id().value(),
          tray.name(),
          tray.position(),
          tray.items().stream().map(ItemJson::from).toList());
    }

    FridgeMemento.TrayState toState() {
      return new FridgeMemento.TrayState(
          new TrayId(id), name, position, items.stream().map(ItemJson::toState).toList());
    }
  }

  record ItemJson(UUID id, String food, BigDecimal grams, BigDecimal tare, LocalDate expiresOn) {

    static ItemJson from(FridgeMemento.ItemState item) {
      return new ItemJson(
          item.id().value(),
          item.foodName(),
          item.quantity().value(),
          item.tare().value(),
          item.expiresOn());
    }

    FridgeMemento.ItemState toState() {
      return new FridgeMemento.ItemState(
          new FoodItemId(id), food, Grams.of(grams), Grams.of(tare), expiresOn);
    }
  }

  record OwnershipJson(UUID item, UUID owner, Visibility visibility, Set<UUID> grantees) {

    static OwnershipJson from(FoodItemId item, Ownership ownership) {
      return new OwnershipJson(
          item.value(),
          ownership.owner().value(),
          ownership.visibility(),
          ownership.grantees().stream().map(MemberId::value).collect(Collectors.toSet()));
    }

    Ownership toOwnership() {
      return new Ownership(
          new MemberId(owner),
          visibility,
          grantees.stream().map(MemberId::new).collect(Collectors.toSet()));
    }
  }
}
