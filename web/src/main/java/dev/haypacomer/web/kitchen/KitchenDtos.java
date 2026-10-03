package dev.haypacomer.web.kitchen;

import dev.haypacomer.application.HayPaComerFacade.KitchenSnapshot;
import dev.haypacomer.application.fridge.FridgeLayout;
import dev.haypacomer.application.inventory.InventoryEntry;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.fridge.FridgeNode;
import dev.haypacomer.domain.fridge.Tray;
import dev.haypacomer.domain.fridge.Zone;
import dev.haypacomer.domain.inventory.FoodStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class KitchenDtos {

  private KitchenDtos() {}

  record SetUpFridgeRequest(@NotBlank @Size(max = 60) String name, FridgeLayout layout) {}

  record NodeResponse(
      UUID id, String type, String name, BigDecimal grams, int items, List<NodeResponse> children) {

    static NodeResponse from(FridgeNode node) {
      return new NodeResponse(
          id(node),
          node.getClass().getSimpleName().toUpperCase(),
          node.name(),
          node.totalGrams().value(),
          node.itemCount(),
          node.children().stream().map(NodeResponse::from).toList());
    }

    private static UUID id(FridgeNode node) {
      return switch (node) {
        case Fridge fridge -> fridge.id().value();
        case Zone zone -> zone.id().value();
        case Tray tray -> tray.id().value();
        case FoodItem item -> item.id().value();
      };
    }
  }

  record InventoryItemResponse(
      UUID id,
      UUID fridgeId,
      UUID trayId,
      String name,
      BigDecimal grams,
      LocalDate expiresOn,
      Set<FoodStatus> statuses,
      boolean edible,
      int rescuePriority) {

    static InventoryItemResponse from(InventoryEntry entry) {
      FoodItem item = entry.food().item();
      return new InventoryItemResponse(
          item.id().value(),
          entry.fridge().value(),
          entry.tray().value(),
          item.name(),
          item.quantity().value(),
          item.expiresOn().orElse(null),
          entry.food().statuses(),
          entry.food().isEdible(),
          entry.food().rescuePriority());
    }
  }

  record SnapshotResponse(int items, BigDecimal totalGrams, int atRisk, int expired) {

    static SnapshotResponse from(KitchenSnapshot snapshot) {
      return new SnapshotResponse(
          snapshot.items(), snapshot.totalGrams().value(), snapshot.atRisk(), snapshot.expired());
    }
  }
}
