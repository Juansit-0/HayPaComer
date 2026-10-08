package dev.haypacomer.application.session;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.CookingSessionRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.session.CookingSession;
import dev.haypacomer.domain.session.CookingSessionId;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class AdvanceCookingSession {

  private final GetHousehold households;
  private final CookingSessionRepository sessions;
  private final Clock clock;

  public AdvanceCookingSession(
      HouseholdRepository households, CookingSessionRepository sessions, Clock clock) {
    this.households = new GetHousehold(households);
    this.sessions = Objects.requireNonNull(sessions, "sessions");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public CookingSession apply(
      UserId actor, HouseholdId householdId, CookingSessionId sessionId, SessionAction action) {
    households.get(actor, householdId).requirePermission(actor, Permission.COOK);
    CookingSession session =
        sessions
            .find(householdId, sessionId)
            .orElseThrow(() -> new CookingSessionNotFoundException(sessionId));
    Instant now = clock.instant();
    switch (action) {
      case NEXT -> session.next(now);
      case PAUSE -> session.pause(now);
      case RESUME -> session.resume(now);
      case ABANDON -> session.abandon(now);
    }
    sessions.save(session);
    return session;
  }
}
