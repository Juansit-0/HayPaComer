package dev.haypacomer.persistence.relational;

import static dev.haypacomer.persistence.relational.PersistenceFixtures.MILK;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.NOW;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.fridge.Tray;
import dev.haypacomer.domain.fridge.Zone;
import dev.haypacomer.domain.fridge.ZoneKind;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.User;
import dev.haypacomer.domain.inventory.InventoryMovement;
import dev.haypacomer.domain.inventory.MovementSource;
import dev.haypacomer.domain.inventory.MovementType;
import dev.haypacomer.domain.inventory.Ownership;
import dev.haypacomer.domain.inventory.Visibility;
import dev.haypacomer.domain.member.MemberId;
import dev.haypacomer.domain.quantity.Grams;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PostgresOwnershipAndMovementsTest extends PostgresTestSupport {

  private Household household;
  private User juan;
  private MemberId juanMember;
  private MemberId anaMember;
  private FoodItem milk;

  @BeforeEach
  void createData() {
    juan = user("juan@haypacomer.dev", "Juan");
    User ana = user("ana@haypacomer.dev", "Ana");
    PostgresUserRepository users = new PostgresUserRepository(dataSource);
    users.save(juan);
    users.save(ana);
    household =
        Household.create(
            "Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan.id(), NOW);
    household.join(ana.id(), Role.MEMBER, NOW);
    new PostgresHouseholdRepository(dataSource).save(household);
    juanMember = household.membershipOf(juan.id()).orElseThrow().member();
    anaMember = household.membershipOf(ana.id()).orElseThrow().member();
    new PostgresFoodCatalogRepository(dataSource).save(MILK);
    Fridge fridge = Fridge.named("Kitchen");
    Zone door = Zone.named("Door", ZoneKind.DOOR);
    Tray rack = Tray.named("Rack", 0);
    door.add(rack);
    fridge.add(door);
    milk = new FoodItem(FoodItemId.newId(), MILK, Grams.of(842), Grams.ZERO, null);
    fridge.place(milk, rack.id());
    new PostgresFridgeRepository(dataSource).save(household.id(), fridge);
  }

  @Test
  void roundTripsOwnershipWithGrants() {
    PostgresFoodOwnershipRepository ownerships = new PostgresFoodOwnershipRepository(dataSource);

    assertTrue(ownerships.find(milk.id()).isEmpty());

    Ownership ownership = Ownership.of(juanMember, Visibility.PRIVATE).grant(anaMember);
    ownerships.save(milk.id(), ownership);
    assertEquals(ownership, ownerships.find(milk.id()).orElseThrow());

    ownerships.save(milk.id(), ownership.revoke(anaMember));
    assertTrue(ownerships.find(milk.id()).orElseThrow().grantees().isEmpty());
    assertThrows(
        IllegalStateException.class,
        () -> ownerships.save(FoodItemId.newId(), Ownership.of(juanMember, Visibility.SHARED)));
  }

  @Test
  void recordsMovementsIdempotently() {
    PostgresInventoryMovementLog log = new PostgresInventoryMovementLog(dataSource);
    InventoryMovement consumed =
        new InventoryMovement(
            UUID.randomUUID(),
            household.id(),
            milk.id(),
            juan.id(),
            MovementType.CONSUME,
            new BigDecimal("-192.00"),
            MovementSource.SCALE,
            NOW);

    log.record(consumed);
    log.record(consumed);

    assertEquals(List.of(consumed), log.history(milk.id()));
  }

  @Test
  void movementsKeepTheirFoodAndAreReadByPeriod() {
    PostgresInventoryMovementLog log = new PostgresInventoryMovementLog(dataSource);
    InventoryMovement rescued =
        new InventoryMovement(
            UUID.randomUUID(),
            household.id(),
            milk.id(),
            juan.id(),
            MovementType.CONSUME,
            new BigDecimal("-200.00"),
            MovementSource.SCALE,
            NOW,
            "milk",
            java.time.LocalDate.of(2026, 10, 10));
    InventoryMovement added =
        new InventoryMovement(
            UUID.randomUUID(),
            household.id(),
            milk.id(),
            juan.id(),
            MovementType.ADD,
            new BigDecimal("842.00"),
            MovementSource.MANUAL,
            NOW.minusSeconds(60),
            "milk",
            null);
    log.record(added);
    log.record(rescued);

    assertEquals(
        List.of(rescued), log.between(household.id(), NOW.minusSeconds(3600), NOW.plusSeconds(1)));
    assertTrue(log.between(household.id(), NOW.plusSeconds(1), NOW.plusSeconds(60)).isEmpty());
  }

  @Test
  void householdPricesOverrideTheColombianReference() {
    PostgresFoodPriceRepository prices = new PostgresFoodPriceRepository(dataSource);
    org.springframework.jdbc.core.simple.JdbcClient.create(dataSource)
        .sql("UPDATE food_catalog SET reference_price_cop_per_kg = 4800 WHERE name_key = 'milk'")
        .update();

    assertEquals(
        0,
        new BigDecimal("4800")
            .compareTo(
                prices
                    .pricesFor(household.id(), java.util.Currency.getInstance("COP"))
                    .get("milk")));
    prices.save(household.id(), "milk", new BigDecimal("5200.00"));
    prices.save(household.id(), "milk", new BigDecimal("5300.00"));

    assertEquals(
        new BigDecimal("5300.00"),
        prices.pricesFor(household.id(), java.util.Currency.getInstance("COP")).get("milk"));
    assertEquals(
        java.util.Map.of("milk", new BigDecimal("5300.00")),
        prices.pricesFor(household.id(), java.util.Currency.getInstance("USD")));
  }

  @Test
  void marketBudgetsAreSavedPerHousehold() {
    PostgresMarketBudgetRepository budgets = new PostgresMarketBudgetRepository(dataSource);

    assertTrue(budgets.monthly(household.id()).isEmpty());
    budgets.save(household.id(), new BigDecimal("600000.00"));
    budgets.save(household.id(), new BigDecimal("650000.00"));

    assertEquals(new BigDecimal("650000.00"), budgets.monthly(household.id()).orElseThrow());
  }
}
