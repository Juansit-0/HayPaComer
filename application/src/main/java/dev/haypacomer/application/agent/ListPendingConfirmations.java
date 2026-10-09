package dev.haypacomer.application.agent;

import dev.haypacomer.application.port.ConfirmationStore;
import dev.haypacomer.domain.identity.UserId;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

public final class ListPendingConfirmations {

  private final ConfirmationStore confirmations;
  private final Clock clock;

  public ListPendingConfirmations(ConfirmationStore confirmations, Clock clock) {
    this.confirmations = Objects.requireNonNull(confirmations, "confirmations");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public List<PendingConfirmation> list(UserId actor) {
    return confirmations.pendingFor(actor).stream()
        .filter(pending -> !pending.expired(clock.instant()))
        .toList();
  }
}
