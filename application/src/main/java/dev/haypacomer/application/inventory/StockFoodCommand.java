package dev.haypacomer.application.inventory;

import dev.haypacomer.domain.expiry.ExpiryEstimate;
import dev.haypacomer.domain.expiry.ExpirySource;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.fridge.TrayId;
import dev.haypacomer.domain.fridge.Zone;
import dev.haypacomer.domain.fridge.ZoneKind;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.inventory.MovementSource;
import dev.haypacomer.domain.inventory.MovementType;
import dev.haypacomer.domain.inventory.Ownership;
import dev.haypacomer.domain.inventory.Visibility;
import dev.haypacomer.domain.quantity.Grams;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record StockFoodCommand(
    UUID id,
    HouseholdId household,
    FridgeId fridge,
    TrayId tray,
    String foodName,
    Grams grossWeight,
    Grams tare,
    LocalDate expiresOn,
    Visibility visibility,
    boolean opened,
    ExpirySource expirySource)
    implements InventoryCommand {

  public StockFoodCommand(
      UUID id,
      HouseholdId household,
      FridgeId fridge,
      TrayId tray,
      String foodName,
      Grams grossWeight,
      Grams tare,
      LocalDate expiresOn,
      Visibility visibility) {
    this(
        id,
        household,
        fridge,
        tray,
        foodName,
        grossWeight,
        tare,
        expiresOn,
        visibility,
        false,
        null);
  }

  public StockFoodCommand {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(household, "household");
    Objects.requireNonNull(fridge, "fridge");
    Objects.requireNonNull(tray, "tray");
    Objects.requireNonNull(foodName, "foodName");
    Objects.requireNonNull(grossWeight, "grossWeight");
    tare = tare == null ? Grams.ZERO : tare;
    visibility = visibility == null ? Visibility.SHARED : visibility;
  }

  @Override
  public String action() {
    return "STOCK_FOOD";
  }

  @Override
  public Map<String, String> detail() {
    return Map.of(
        "food", foodName,
        "grossGrams", grossWeight.value().toPlainString(),
        "tareGrams", tare.value().toPlainString(),
        "visibility", visibility.name(),
        "opened", String.valueOf(opened));
  }

  @Override
  public CommandOutcome execute(InventoryWorkspace workspace) {
    workspace
        .household()
        .requirePermission(
            workspace.actor(),
            visibility == Visibility.SHARED
                ? Permission.EDIT_INVENTORY
                : Permission.MANAGE_OWN_ITEMS);
    FoodMetadata food =
        workspace
            .catalog()
            .findByName(foodName)
            .orElseThrow(() -> new FoodNotInCatalogException(foodName));
    Fridge target =
        workspace.inventory().fridges(household).stream()
            .filter(candidate -> candidate.id().equals(fridge))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown fridge for this household"));
    ZoneKind zone =
        target
            .zoneOf(tray)
            .map(Zone::kind)
            .orElseThrow(() -> new IllegalArgumentException("Tray not in fridge " + tray.value()));
    LocalDate today = LocalDate.ofInstant(workspace.now(), workspace.household().timezone());
    Optional<ExpiryEstimate> expiry =
        workspace.expiry().resolve(household, food, zone, opened, expiresOn, expirySource, today);
    FoodItem item =
        expiry
            .map(found -> FoodItem.weighed(food, grossWeight, tare, found, opened ? today : null))
            .orElseGet(() -> FoodItem.weighed(food, grossWeight, tare, null));
    target.place(item, tray);
    workspace.inventory().save(household, target);
    workspace
        .inventory()
        .ownerships()
        .save(
            item.id(),
            Ownership.of(
                workspace.household().membershipOf(workspace.actor()).orElseThrow().member(),
                visibility));
    workspace
        .inventory()
        .record(
            id,
            household,
            item,
            workspace.actor(),
            MovementType.ADD,
            item.quantity().value(),
            MovementSource.MANUAL,
            workspace.now());
    return new CommandOutcome(id, item.id(), item.quantity(), false);
  }
}
