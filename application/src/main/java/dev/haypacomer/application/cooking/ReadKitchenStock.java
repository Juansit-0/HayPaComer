package dev.haypacomer.application.cooking;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.inventory.InventoryEntry;
import dev.haypacomer.application.inventory.ViewInventory;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.cooking.Availability;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.FoodStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class ReadKitchenStock {

  private final GetHousehold households;
  private final ViewInventory inventory;
  private final Clock clock;

  public ReadKitchenStock(HouseholdRepository households, ViewInventory inventory, Clock clock) {
    this.households = new GetHousehold(households);
    this.inventory = Objects.requireNonNull(inventory, "inventory");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public KitchenStock read(UserId actor, HouseholdId householdId) {
    Household household = households.get(actor, householdId);
    LocalDate today = LocalDate.ofInstant(clock.instant(), household.timezone());
    Availability availability = new Availability();
    Set<String> atRisk = new HashSet<>();
    inventory.view(actor, householdId, today).stream()
        .filter(InventoryEntry::usable)
        .filter(entry -> entry.food().isEdible())
        .forEach(
            entry -> {
              availability.add(entry.food().item().food(), entry.food().item().quantity());
              if (entry.food().has(FoodStatus.AT_RISK) || entry.food().has(FoodStatus.LEFTOVER)) {
                atRisk.add(entry.food().item().food().key());
              }
            });
    return new KitchenStock(availability, atRisk);
  }
}
