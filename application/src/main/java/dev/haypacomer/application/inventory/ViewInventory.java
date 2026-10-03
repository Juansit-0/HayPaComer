package dev.haypacomer.application.inventory;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.FreshnessPolicy;
import dev.haypacomer.domain.inventory.PlainFood;
import dev.haypacomer.domain.inventory.StockedFood;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class ViewInventory {

  private static final Comparator<InventoryEntry> RESCUE_ORDER =
      Comparator.comparing(InventoryEntry::food, StockedFood.RESCUE_ORDER);

  private final GetHousehold households;
  private final FridgeRepository fridges;
  private final FreshnessPolicy freshness;

  public ViewInventory(
      HouseholdRepository households, FridgeRepository fridges, FreshnessPolicy freshness) {
    this.households = new GetHousehold(households);
    this.fridges = Objects.requireNonNull(fridges, "fridges");
    this.freshness = Objects.requireNonNull(freshness, "freshness");
  }

  public List<InventoryEntry> view(UserId actor, HouseholdId householdId, LocalDate today) {
    households.get(actor, householdId);
    return fridges.findByHousehold(householdId).stream()
        .flatMap(fridge -> entries(fridge, today).stream())
        .sorted(RESCUE_ORDER)
        .toList();
  }

  private List<InventoryEntry> entries(Fridge fridge, LocalDate today) {
    return fridge
        .trays()
        .flatMap(
            tray ->
                tray.children().stream()
                    .map(
                        item ->
                            new InventoryEntry(
                                fridge.id(),
                                tray.id(),
                                freshness.apply(new PlainFood(item), today))))
        .toList();
  }
}
