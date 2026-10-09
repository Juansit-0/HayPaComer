package dev.haypacomer.application.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.fridge.FridgeLayout;
import dev.haypacomer.application.fridge.SetUpFridge;
import dev.haypacomer.application.household.HouseholdNotFoundException;
import dev.haypacomer.application.live.BroadcastLiveUpdate;
import dev.haypacomer.application.settings.FixedPolicies;
import dev.haypacomer.application.support.InMemoryFridgeRepository;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.application.support.InMemoryInventoryStores;
import dev.haypacomer.application.support.InMemorySnapshotStore;
import dev.haypacomer.domain.expiry.ExpiryEstimate;
import dev.haypacomer.domain.expiry.ExpiryRules;
import dev.haypacomer.domain.expiry.ExpirySource;
import dev.haypacomer.domain.expiry.ImpossibleExpiryException;
import dev.haypacomer.domain.expiry.ShelfLife;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.fridge.Tray;
import dev.haypacomer.domain.fridge.ZoneKind;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.Visibility;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExpiryInventoryTest {

  private static final Instant NOW = Instant.parse("2026-10-09T15:00:00Z");
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 9);
  private static final FoodMetadata MILK =
      new FoodMetadata(
          "Milk", FoodCategory.DAIRY, Unit.GRAM, ConversionFactors.MASS_ONLY, true, 7, Set.of());

  private final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  private final InMemoryFridgeRepository fridges = new InMemoryFridgeRepository();
  private final InMemoryInventoryStores stores = new InMemoryInventoryStores();
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final UserId juan = UserId.newId();
  private final ExpiryDesk desk =
      ExpiryDesk.enforcing(food -> new ShelfLife(7, 5, 90, 4), FixedPolicies.DEFAULT);
  private Household household;
  private Fridge fridge;
  private Tray top;
  private Tray door;
  private ExecuteInventoryCommand commands;

  @BeforeEach
  void createKitchen() {
    household =
        Household.create("Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan, NOW);
    households.save(household);
    fridge =
        new SetUpFridge(households, fridges)
            .setUp(juan, household.id(), "Kitchen", FridgeLayout.STANDARD);
    top = fridge.trays().filter(tray -> tray.name().equals("Top")).findFirst().orElseThrow();
    door = fridge.trays().filter(tray -> tray.name().equals("Rack")).findFirst().orElseThrow();
    stores.catalog.save(MILK);
    commands =
        new ExecuteInventoryCommand(
            households,
            fridges,
            stores.ownerships,
            stores.movements,
            stores.catalog,
            new FoodAccessGuard(),
            stores.audit,
            stores.unitOfWork,
            new InMemorySnapshotStore(),
            BroadcastLiveUpdate.NOBODY,
            desk,
            clock);
  }

  private CommandOutcome stock(Tray tray, LocalDate date, boolean opened, ExpirySource source) {
    return commands.execute(
        juan,
        new StockFoodCommand(
            UUID.randomUUID(),
            household.id(),
            fridge.id(),
            tray.id(),
            "Milk",
            Grams.of(900),
            Grams.of(50),
            date,
            Visibility.SHARED,
            opened,
            source));
  }

  private FoodItem item(CommandOutcome outcome) {
    return fridges
        .findByHousehold(household.id())
        .getFirst()
        .findItem(outcome.item())
        .orElseThrow();
  }

  @Test
  void foodWithoutADateGetsAnEstimateByWhereItIsStored() {
    FoodItem shelf = item(stock(top, null, false, null));
    FoodItem inDoor = item(stock(door, null, false, null));
    FoodItem opened = item(stock(top, null, true, null));

    assertEquals(TODAY.plusDays(7), shelf.expiresOn().orElseThrow());
    assertEquals(ExpirySource.ESTIMATED, shelf.expirySource().orElseThrow());
    assertEquals(TODAY.plusDays(5), inDoor.expiresOn().orElseThrow());
    assertEquals(TODAY.plusDays(4), opened.expiresOn().orElseThrow());
    assertEquals(TODAY, opened.openedOn().orElseThrow());
  }

  @Test
  void impossibleDatesAreRejectedAndRealOnesKeepTheirSource() {
    assertThrows(
        ImpossibleExpiryException.class, () -> stock(top, TODAY.plusDays(40), false, null));
    assertTrue(fridges.findByHousehold(household.id()).getFirst().foodItems().findAny().isEmpty());

    FoodItem typed = item(stock(top, TODAY.plusDays(6), false, null));
    FoodItem label = item(stock(top, TODAY.plusDays(5), false, ExpirySource.LABEL));

    assertEquals(ExpirySource.USER, typed.expirySource().orElseThrow());
    assertEquals(ExpirySource.LABEL, label.expirySource().orElseThrow());
  }

  @Test
  void theLenientDeskKeepsWhatWasTyped() {
    assertEquals(
        TODAY.plusDays(40),
        ExpiryDesk.LENIENT
            .resolve(household.id(), MILK, ZoneKind.SHELF, false, TODAY.plusDays(40), null, TODAY)
            .orElseThrow()
            .date());
    assertTrue(
        ExpiryDesk.LENIENT
            .resolve(household.id(), MILK, ZoneKind.SHELF, false, null, null, TODAY)
            .isEmpty());
  }

  @Test
  void estimatesArePreviewedForMembersOnly() {
    EstimateExpiry estimate = new EstimateExpiry(households, stores.catalog, desk, clock);

    ExpiryEstimate door = estimate.estimate(juan, household.id(), "Milk", ZoneKind.DOOR, false);

    assertEquals(TODAY.plusDays(5), door.date());
    assertEquals(
        TODAY.plusDays(90),
        estimate.estimate(juan, household.id(), "milk", ZoneKind.FREEZER, false).date());
    assertThrows(
        FoodNotInCatalogException.class,
        () -> estimate.estimate(juan, household.id(), "Unicorn", ZoneKind.SHELF, false));
    assertThrows(
        HouseholdNotFoundException.class,
        () -> estimate.estimate(UserId.newId(), household.id(), "Milk", ZoneKind.SHELF, false));
  }

  @Test
  void openingBringsTheDateCloserAndIsAudited() {
    MarkFoodOpened open =
        new MarkFoodOpened(
            households,
            fridges,
            stores.ownerships,
            stores.movements,
            new FoodAccessGuard(),
            ExpiryDesk.enforcing(
                food -> new ShelfLife(7, 5, 90, 4),
                FixedPolicies.DEFAULT.withExpiryRules(new ExpiryRules(3))),
            stores.audit,
            stores.unitOfWork,
            clock);
    CommandOutcome typed = stock(top, TODAY.plusDays(7), false, null);
    CommandOutcome soon = stock(top, TODAY.plusDays(2), false, null);

    FoodItem opened = open.open(juan, household.id(), typed.item());
    FoodItem kept = open.open(juan, household.id(), soon.item());

    assertEquals(TODAY.plusDays(4), opened.expiresOn().orElseThrow());
    assertEquals(ExpirySource.ESTIMATED, opened.expirySource().orElseThrow());
    assertEquals(TODAY.plusDays(4), item(typed).expiresOn().orElseThrow());
    assertEquals(TODAY.plusDays(2), kept.expiresOn().orElseThrow());
    assertEquals(ExpirySource.USER, kept.expirySource().orElseThrow());
    assertEquals("OPEN_FOOD", stores.auditEntries.getLast().action());
    assertThrows(
        HouseholdNotFoundException.class,
        () -> open.open(UserId.newId(), household.id(), typed.item()));
    assertThrows(
        FoodItemNotFoundException.class,
        () ->
            open.open(
                juan,
                household.id(),
                new dev.haypacomer.domain.fridge.FoodItemId(UUID.randomUUID())));
  }
}
