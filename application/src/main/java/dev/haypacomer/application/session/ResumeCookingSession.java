package dev.haypacomer.application.session;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.CookingSessionRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.session.CookingSession;
import java.util.Objects;
import java.util.Optional;

public final class ResumeCookingSession {

  private final GetHousehold households;
  private final CookingSessionRepository sessions;

  public ResumeCookingSession(HouseholdRepository households, CookingSessionRepository sessions) {
    this.households = new GetHousehold(households);
    this.sessions = Objects.requireNonNull(sessions, "sessions");
  }

  public Optional<CookingSession> active(UserId actor, HouseholdId householdId) {
    households.get(actor, householdId);
    return sessions.active(householdId);
  }
}
