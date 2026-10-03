package dev.haypacomer.application;

import dev.haypacomer.application.fridge.FridgeLayout;
import dev.haypacomer.application.fridge.ListFridges;
import dev.haypacomer.application.fridge.SetUpFridge;
import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.inventory.InventoryEntry;
import dev.haypacomer.application.inventory.ViewInventory;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.FoodStatus;
import dev.haypacomer.domain.quantity.Grams;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public final class HayPaComerFacade {

  private final GetHousehold getHousehold;
  private final SetUpFridge setUpFridge;
  private final ListFridges listFridges;
  private final ViewInventory viewInventory;
  private final Clock clock;

  public HayPaComerFacade(
      GetHousehold getHousehold,
      SetUpFridge setUpFridge,
      ListFridges listFridges,
      ViewInventory viewInventory,
      Clock clock) {
    this.getHousehold = Objects.requireNonNull(getHousehold, "getHousehold");
    this.setUpFridge = Objects.requireNonNull(setUpFridge, "setUpFridge");
    this.listFridges = Objects.requireNonNull(listFridges, "listFridges");
    this.viewInventory = Objects.requireNonNull(viewInventory, "viewInventory");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public Fridge setUpFridge(UserId actor, HouseholdId household, String name, FridgeLayout layout) {
    return setUpFridge.setUp(actor, household, name, layout);
  }

  public List<Fridge> fridges(UserId actor, HouseholdId household) {
    return listFridges.list(actor, household);
  }

  public List<InventoryEntry> inventory(UserId actor, HouseholdId household) {
    return viewInventory.view(actor, household, today(actor, household));
  }

  public List<InventoryEntry> rescueFirst(UserId actor, HouseholdId household) {
    return inventory(actor, household).stream()
        .filter(InventoryEntry::usable)
        .filter(entry -> entry.food().isEdible())
        .filter(entry -> entry.food().rescuePriority() > 0)
        .toList();
  }

  public KitchenSnapshot snapshot(UserId actor, HouseholdId household) {
    List<InventoryEntry> entries = inventory(actor, household);
    Grams total =
        entries.stream()
            .map(entry -> entry.food().item().quantity())
            .reduce(Grams.ZERO, Grams::plus);
    long atRisk = entries.stream().filter(entry -> entry.food().has(FoodStatus.AT_RISK)).count();
    long expired = entries.stream().filter(entry -> entry.food().has(FoodStatus.EXPIRED)).count();
    return new KitchenSnapshot(entries.size(), total, (int) atRisk, (int) expired);
  }

  private LocalDate today(UserId actor, HouseholdId householdId) {
    Household household = getHousehold.get(actor, householdId);
    return LocalDate.ofInstant(clock.instant(), household.timezone());
  }

  public record KitchenSnapshot(int items, Grams totalGrams, int atRisk, int expired) {}
}
