package dev.haypacomer.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.fridge.FridgeLayout;
import dev.haypacomer.application.fridge.ListFridges;
import dev.haypacomer.application.fridge.SetUpFridge;
import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.household.HouseholdNotFoundException;
import dev.haypacomer.application.inventory.InventoryEntry;
import dev.haypacomer.application.inventory.ViewInventory;
import dev.haypacomer.application.support.InMemoryFridgeRepository;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.fridge.FridgeNode;
import dev.haypacomer.domain.fridge.Traversal;
import dev.haypacomer.domain.fridge.Tray;
import dev.haypacomer.domain.household.AccessDeniedException;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.FoodStatus;
import dev.haypacomer.domain.inventory.FreshnessPolicy;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HayPaComerFacadeTest {

  private static final Instant NOW = Instant.parse("2026-10-04T03:00:00Z");
  private static final LocalDate TODAY_IN_BOGOTA = LocalDate.of(2026, 10, 3);

  private final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  private final InMemoryFridgeRepository fridges = new InMemoryFridgeRepository();
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final UserId juan = UserId.newId();
  private final UserId ana = UserId.newId();
  private Household household;
  private HayPaComerFacade facade;

  private static FoodMetadata food(String name) {
    return new FoodMetadata(
        name, FoodCategory.PREPARED, Unit.GRAM, ConversionFactors.MASS_ONLY, true, 3, Set.of());
  }

  @BeforeEach
  void createFacade() {
    household =
        Household.create(
            "Apartment", Currency.getInstance("COP"), ZoneId.of("America/Bogota"), juan, NOW);
    household.join(ana, Role.MEMBER, NOW);
    households.save(household);
    facade =
        new HayPaComerFacade(
            new GetHousehold(households),
            new SetUpFridge(households, fridges),
            new ListFridges(households, fridges),
            new ViewInventory(households, fridges, FreshnessPolicy.DEFAULT),
            clock);
  }

  private void stock(Fridge fridge, String trayName, String name, long grams, LocalDate expiry) {
    Tray tray = fridge.trays().filter(t -> t.name().equals(trayName)).findFirst().orElseThrow();
    fridge.place(
        new FoodItem(FoodItemId.newId(), food(name), Grams.of(grams), Grams.ZERO, expiry),
        tray.id());
  }

  @Test
  void setsUpAStandardFridgeForOwnersOnly() {
    Fridge fridge = facade.setUpFridge(juan, household.id(), "Kitchen", FridgeLayout.STANDARD);

    assertEquals(
        List.of(
            "Kitchen",
            "Shelves",
            "Top",
            "Middle",
            "Bottom",
            "Door",
            "Rack",
            "Drawer",
            "Vegetables"),
        fridge.nodes(Traversal.DEPTH_FIRST).map(FridgeNode::name).toList());
    assertEquals(List.of(fridge), facade.fridges(ana, household.id()));
    assertEquals(
        0, facade.setUpFridge(juan, household.id(), "Garage", FridgeLayout.EMPTY).itemCount());
    assertThrows(
        AccessDeniedException.class,
        () -> facade.setUpFridge(ana, household.id(), "Mine", FridgeLayout.STANDARD));
    assertThrows(
        HouseholdNotFoundException.class, () -> facade.fridges(UserId.newId(), household.id()));
  }

  @Test
  void inventoryUsesTheHouseholdTimezoneAndRescueOrder() {
    Fridge kitchen = facade.setUpFridge(juan, household.id(), "Kitchen", FridgeLayout.STANDARD);
    Fridge garage = facade.setUpFridge(juan, household.id(), "Garage", FridgeLayout.STANDARD);
    stock(kitchen, "Top", "Carrots", 400, TODAY_IN_BOGOTA.plusDays(9));
    stock(kitchen, "Rack", "Old milk", 650, TODAY_IN_BOGOTA.minusDays(1));
    stock(garage, "Bottom", "Chicken", 80, TODAY_IN_BOGOTA.plusDays(1));
    stock(garage, "Middle", "Soup", 300, TODAY_IN_BOGOTA);
    stock(kitchen, "Vegetables", "Rice", 1000, null);

    List<InventoryEntry> inventory = facade.inventory(ana, household.id());

    assertEquals(
        List.of("Soup", "Chicken", "Carrots", "Rice", "Old milk"),
        inventory.stream().map(entry -> entry.food().item().name()).toList());
    assertTrue(inventory.getFirst().food().has(FoodStatus.AT_RISK));
    assertEquals(garage.id(), inventory.getFirst().fridge());
    assertEquals(
        List.of("Soup", "Chicken"),
        facade.rescueFirst(ana, household.id()).stream().map(e -> e.food().item().name()).toList());
  }

  @Test
  void snapshotSummarisesTheKitchen() {
    Fridge kitchen = facade.setUpFridge(juan, household.id(), "Kitchen", FridgeLayout.STANDARD);
    stock(kitchen, "Top", "Soup", 300, TODAY_IN_BOGOTA);
    stock(kitchen, "Rack", "Old milk", 650, TODAY_IN_BOGOTA.minusDays(3));
    stock(kitchen, "Bottom", "Rice", 1000, null);

    HayPaComerFacade.KitchenSnapshot snapshot = facade.snapshot(juan, household.id());

    assertEquals(new HayPaComerFacade.KitchenSnapshot(3, Grams.of(1950), 1, 1), snapshot);
  }
}
