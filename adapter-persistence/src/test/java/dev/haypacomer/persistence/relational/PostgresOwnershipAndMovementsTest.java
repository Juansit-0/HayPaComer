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
}
