package dev.haypacomer.application.session;

import dev.haypacomer.application.port.CookingSessionRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.StepTimerStore;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.session.CookingSessionId;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class ViewStepTimer {

  private final ViewCookingSession sessions;
  private final StepTimerStore timers;
  private final Clock clock;

  public ViewStepTimer(
      HouseholdRepository households,
      CookingSessionRepository sessions,
      StepTimerStore timers,
      Clock clock) {
    this.sessions = new ViewCookingSession(households, sessions);
    this.timers = Objects.requireNonNull(timers, "timers");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public Optional<TimerStatus> view(
      UserId actor, HouseholdId householdId, CookingSessionId sessionId) {
    sessions.view(actor, householdId, sessionId);
    Instant now = clock.instant();
    return timers
        .find(sessionId)
        .map(
            timer ->
                new TimerStatus(
                    timer.step(),
                    timer.duration(),
                    timer.remaining(now),
                    timer.paused(),
                    timer.remaining(now).isZero()));
  }
}
