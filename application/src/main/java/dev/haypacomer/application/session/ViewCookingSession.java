package dev.haypacomer.application.session;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.CookingSessionRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.session.CookingSession;
import dev.haypacomer.domain.session.CookingSessionId;
import java.util.Objects;

public final class ViewCookingSession {

  private final GetHousehold households;
  private final CookingSessionRepository sessions;

  public ViewCookingSession(HouseholdRepository households, CookingSessionRepository sessions) {
    this.households = new GetHousehold(households);
    this.sessions = Objects.requireNonNull(sessions, "sessions");
  }

  public CookingSession view(UserId actor, HouseholdId householdId, CookingSessionId sessionId) {
    households.get(actor, householdId);
    return sessions
        .find(householdId, sessionId)
        .orElseThrow(() -> new CookingSessionNotFoundException(sessionId));
  }
}
