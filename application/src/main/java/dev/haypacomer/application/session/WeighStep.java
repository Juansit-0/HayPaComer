package dev.haypacomer.application.session;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.CookingSessionRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.scale.ReadWeighingProgress;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.scale.WeighingProgress;
import dev.haypacomer.domain.session.CookingSession;
import dev.haypacomer.domain.session.CookingSessionId;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class WeighStep {

  private final GetHousehold households;
  private final CookingSessionRepository sessions;
  private final ReadWeighingProgress scaleProgress;
  private final KitchenMediator mediator;
  private final Clock clock;

  public WeighStep(
      HouseholdRepository households,
      CookingSessionRepository sessions,
      ReadWeighingProgress scaleProgress,
      KitchenMediator mediator,
      Clock clock) {
    this.households = new GetHousehold(households);
    this.sessions = Objects.requireNonNull(sessions, "sessions");
    this.scaleProgress = Objects.requireNonNull(scaleProgress, "scaleProgress");
    this.mediator = Objects.requireNonNull(mediator, "mediator");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public StepWeighingResult weigh(
      UserId actor,
      HouseholdId householdId,
      CookingSessionId sessionId,
      int position,
      Optional<Grams> manual) {
    households.get(actor, householdId).requirePermission(actor, Permission.COOK);
    CookingSession session =
        sessions
            .find(householdId, sessionId)
            .orElseThrow(() -> new CookingSessionNotFoundException(sessionId));
    session.weighingTargetFor(position);
    Grams measured =
        manual.orElseGet(
            () ->
                scaleProgress
                    .read(
                        actor,
                        householdId,
                        session
                            .scale()
                            .orElseThrow(
                                () ->
                                    new IllegalArgumentException(
                                        "Send the grams or attach a scale to the session")))
                    .measured());
    Instant now = clock.instant();
    WeighingProgress progress = session.weigh(position, measured, now);
    sessions.save(session);
    mediator.notify(new KitchenEvent.StepWeighed(session, progress, now));
    return new StepWeighingResult(session, progress);
  }
}
