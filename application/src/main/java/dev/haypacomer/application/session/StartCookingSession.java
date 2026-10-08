package dev.haypacomer.application.session;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.CookingSessionRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.session.CookingSession;
import java.time.Clock;
import java.util.Objects;

public final class StartCookingSession {

  private final GetHousehold households;
  private final CookingSessionRepository sessions;
  private final Clock clock;

  public StartCookingSession(
      HouseholdRepository households, CookingSessionRepository sessions, Clock clock) {
    this.households = new GetHousehold(households);
    this.sessions = Objects.requireNonNull(sessions, "sessions");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public CookingSession start(UserId actor, HouseholdId householdId, Recipe recipe, int servings) {
    households.get(actor, householdId).requirePermission(actor, Permission.COOK);
    sessions
        .active(householdId)
        .ifPresent(
            active -> {
              throw new SessionAlreadyActiveException(active.id());
            });
    CookingSession session =
        CookingSession.start(householdId, recipe, servings, actor, clock.instant());
    sessions.save(session);
    return session;
  }
}
