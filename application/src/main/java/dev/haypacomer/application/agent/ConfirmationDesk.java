package dev.haypacomer.application.agent;

import dev.haypacomer.application.port.AgentRunStore;
import dev.haypacomer.application.port.ConfirmationStore;
import dev.haypacomer.domain.identity.UserId;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public record ConfirmationDesk(ConfirmationStore confirmations, AgentRunStore runs, Clock clock) {

  public ConfirmationDesk {
    Objects.requireNonNull(confirmations, "confirmations");
    Objects.requireNonNull(runs, "runs");
    Objects.requireNonNull(clock, "clock");
  }

  public PendingConfirmation take(UserId actor, UUID id) {
    PendingConfirmation pending =
        confirmations
            .find(id)
            .filter(found -> found.user().equals(actor))
            .orElseThrow(ConfirmationNotFoundException::new);
    confirmations.remove(pending);
    if (pending.expired(clock.instant())) {
      close(pending, RunStatus.FAILED, "Confirmation expired before anyone answered");
      throw new ConfirmationExpiredException();
    }
    return pending;
  }

  public void close(PendingConfirmation pending, RunStatus status, String detail) {
    runs.trace(pending.run(), new TraceStep(TraceKind.OBSERVATION, detail, clock.instant()));
    runs.find(pending.run())
        .ifPresent(run -> runs.save(run.advance(status, run.stepsUsed(), clock.instant())));
  }
}
