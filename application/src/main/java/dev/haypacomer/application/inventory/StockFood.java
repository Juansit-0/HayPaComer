package dev.haypacomer.application.inventory;

import dev.haypacomer.application.port.FoodCatalogRepository;
import dev.haypacomer.application.port.FoodOwnershipRepository;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.InventoryMovementLog;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.MovementSource;
import dev.haypacomer.domain.inventory.MovementType;
import dev.haypacomer.domain.inventory.Ownership;
import dev.haypacomer.domain.inventory.Visibility;
import dev.haypacomer.domain.member.MemberId;
import dev.haypacomer.domain.quantity.Grams;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class StockFood {

  private final HouseholdInventory inventory;
  private final FridgeRepository fridges;
  private final FoodCatalogRepository catalog;
  private final Clock clock;

  public StockFood(
      HouseholdRepository households,
      FridgeRepository fridges,
      FoodOwnershipRepository ownerships,
      InventoryMovementLog movements,
      FoodCatalogRepository catalog,
      Clock clock) {
    this.inventory = new HouseholdInventory(households, fridges, ownerships, movements);
    this.fridges = Objects.requireNonNull(fridges, "fridges");
    this.catalog = Objects.requireNonNull(catalog, "catalog");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public FoodItem stock(UserId actor, HouseholdId householdId, StockCommand command) {
    Household household = inventory.household(actor, householdId);
    Visibility visibility = command.visibility() == null ? Visibility.SHARED : command.visibility();
    household.requirePermission(
        actor,
        visibility == Visibility.SHARED ? Permission.EDIT_INVENTORY : Permission.MANAGE_OWN_ITEMS);
    FoodMetadata food =
        catalog
            .findByName(command.foodName())
            .orElseThrow(() -> new FoodNotInCatalogException(command.foodName()));
    Fridge fridge =
        fridges.findByHousehold(householdId).stream()
            .filter(candidate -> candidate.id().equals(command.fridge()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown fridge for this household"));
    Grams tare = command.tare() == null ? Grams.ZERO : command.tare();
    FoodItem item = FoodItem.weighed(food, command.grossWeight(), tare, command.expiresOn());
    fridge.place(item, command.tray());
    inventory.save(householdId, fridge);
    MemberId owner = household.membershipOf(actor).orElseThrow().member();
    inventory.ownerships().save(item.id(), Ownership.of(owner, visibility));
    Instant now = clock.instant();
    inventory.record(
        householdId,
        item.id(),
        actor,
        MovementType.ADD,
        item.quantity().value(),
        MovementSource.MANUAL,
        now);
    return item;
  }
}
