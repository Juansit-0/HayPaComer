package dev.haypacomer.persistence.relational;

import static dev.haypacomer.persistence.relational.PersistenceFixtures.NOW;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.User;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PostgresHouseholdRepositoryTest extends PostgresTestSupport {

  private PostgresHouseholdRepository households;
  private User juan;
  private User ana;
  private Household apartment;

  @BeforeEach
  void createData() {
    PostgresUserRepository users = new PostgresUserRepository(dataSource);
    households = new PostgresHouseholdRepository(dataSource);
    juan = user("juan@haypacomer.dev", "Juan");
    ana = user("ana@haypacomer.dev", "Ana");
    users.save(juan);
    users.save(ana);
    apartment =
        Household.create(
            "Apartment 402",
            Currency.getInstance("COP"),
            ZoneId.of("America/Bogota"),
            juan.id(),
            NOW);
    apartment.join(ana.id(), Role.MEMBER, NOW);
  }

  @Test
  void savesAndRestoresMemberships() {
    households.save(apartment);

    Household loaded = households.findById(apartment.id()).orElseThrow();

    assertEquals("Apartment 402", loaded.name());
    assertEquals(Currency.getInstance("COP"), loaded.currency());
    assertEquals(ZoneId.of("America/Bogota"), loaded.timezone());
    assertEquals(juan.id(), loaded.owner());
    assertEquals(
        apartment.membershipOf(ana.id()).orElseThrow(),
        loaded.membershipOf(ana.id()).orElseThrow());
  }

  @Test
  void persistsOwnershipTransfer() {
    households.save(apartment);

    apartment.transferOwnership(juan.id(), ana.id());
    households.save(apartment);

    Household loaded = households.findById(apartment.id()).orElseThrow();
    assertEquals(ana.id(), loaded.owner());
    assertEquals(Role.MEMBER, loaded.membershipOf(juan.id()).orElseThrow().role());
  }

  @Test
  void persistsRemovalAndRename() {
    households.save(apartment);

    apartment.remove(juan.id(), ana.id());
    apartment.rename(juan.id(), "Casa");
    households.save(apartment);

    Household loaded = households.findById(apartment.id()).orElseThrow();
    assertEquals("Casa", loaded.name());
    assertEquals(1, loaded.memberships().size());
  }

  @Test
  void findsHouseholdsOfAUser() {
    households.save(apartment);

    assertEquals(
        List.of(apartment.id()),
        households.findByUser(ana.id()).stream().map(Household::id).toList());
    assertTrue(households.findById(HouseholdId.newId()).isEmpty());
  }
}
