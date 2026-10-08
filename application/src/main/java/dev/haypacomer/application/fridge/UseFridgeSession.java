package dev.haypacomer.application.fridge;

import dev.haypacomer.application.coldchain.FridgeNotFoundException;
import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.application.port.FridgeSessionRegistry;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.fridge.FridgeSession;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class UseFridgeSession {

  private final GetHousehold households;
  private final FridgeRepository fridges;
  private final FridgeSessionRegistry sessions;
  private final Clock clock;

  public UseFridgeSession(
      HouseholdRepository households,
      FridgeRepository fridges,
      FridgeSessionRegistry sessions,
      Clock clock) {
    this.households = new GetHousehold(households);
    this.fridges = Objects.requireNonNull(fridges, "fridges");
    this.sessions = Objects.requireNonNull(sessions, "sessions");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public FridgeSessionStatus apply(
      UserId actor, HouseholdId householdId, FridgeId fridgeId, FridgeSessionAction action) {
    Household household = households.get(actor, householdId);
    fridges.findByHousehold(householdId).stream()
        .filter(fridge -> fridge.id().equals(fridgeId))
        .findFirst()
        .orElseThrow(FridgeNotFoundException::new);
    FridgeSession session = sessions.sessionOf(fridgeId);
    Instant now = clock.instant();
    switch (action) {
      case VIEW -> {}
      case CLAIM -> {
        household.requirePermission(actor, Permission.MANAGE_OWN_ITEMS);
        session.claim(actor, now);
      }
      case RELEASE -> session.release(actor, now);
    }
    return new FridgeSessionStatus(
        fridgeId, session.activeUser(now).orElse(null), session.expiresAt(now).orElse(null));
  }
}
