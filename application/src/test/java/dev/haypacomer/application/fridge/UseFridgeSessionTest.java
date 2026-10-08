package dev.haypacomer.application.fridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.coldchain.FridgeNotFoundException;
import dev.haypacomer.application.household.HouseholdNotFoundException;
import dev.haypacomer.application.support.InMemoryFridgeRepository;
import dev.haypacomer.application.support.InMemoryFridgeSessions;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.fridge.FridgeBusyException;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.fridge.FridgeSession;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UseFridgeSessionTest {

  private static final Instant NOW = Instant.parse("2026-10-08T20:00:00Z");

  private final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  private final InMemoryFridgeRepository fridges = new InMemoryFridgeRepository();
  private final InMemoryFridgeSessions sessions = new InMemoryFridgeSessions();
  private final UseFridgeSession use =
      new UseFridgeSession(households, fridges, sessions, Clock.fixed(NOW, ZoneOffset.UTC));
  private final UserId juan = UserId.newId();
  private final UserId ana = UserId.newId();
  private Household household;
  private Fridge fridge;

  @BeforeEach
  void setUp() {
    household =
        Household.create("Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan, NOW);
    household.join(ana, Role.GUEST, NOW);
    households.save(household);
    fridge =
        new SetUpFridge(households, fridges)
            .setUp(juan, household.id(), "Kitchen", FridgeLayout.STANDARD);
  }

  @Test
  void claimsViewsAndReleasesTheSingleSession() {
    FridgeSessionStatus claimed =
        use.apply(juan, household.id(), fridge.id(), FridgeSessionAction.CLAIM);

    assertEquals(juan, claimed.activeUser().orElseThrow());
    assertEquals(NOW.plus(FridgeSession.IDLE_TIMEOUT), claimed.expiresAt());
    assertEquals(
        juan,
        use.apply(ana, household.id(), fridge.id(), FridgeSessionAction.VIEW)
            .activeUser()
            .orElseThrow());
    assertThrows(
        FridgeBusyException.class,
        () -> use.apply(ana, household.id(), fridge.id(), FridgeSessionAction.CLAIM));
    assertTrue(
        use.apply(juan, household.id(), fridge.id(), FridgeSessionAction.RELEASE)
            .activeUser()
            .isEmpty());
    assertEquals(
        ana,
        use.apply(ana, household.id(), fridge.id(), FridgeSessionAction.CLAIM)
            .activeUser()
            .orElseThrow());
  }

  @Test
  void onlyFridgesOfTheHouseholdAndOnlyMembers() {
    assertThrows(
        FridgeNotFoundException.class,
        () -> use.apply(juan, household.id(), FridgeId.newId(), FridgeSessionAction.VIEW));
    assertThrows(
        HouseholdNotFoundException.class,
        () -> use.apply(UserId.newId(), household.id(), fridge.id(), FridgeSessionAction.CLAIM));
  }
}
