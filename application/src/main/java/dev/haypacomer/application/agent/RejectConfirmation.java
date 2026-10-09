package dev.haypacomer.application.agent;

import dev.haypacomer.domain.identity.UserId;
import java.util.Objects;
import java.util.UUID;

public final class RejectConfirmation {

  private final ConfirmationDesk desk;

  public RejectConfirmation(ConfirmationDesk desk) {
    this.desk = Objects.requireNonNull(desk, "desk");
  }

  public PendingConfirmation reject(UserId actor, UUID id) {
    PendingConfirmation pending = desk.take(actor, id);
    desk.close(pending, RunStatus.DONE, "Rejected by the person: " + pending.summary());
    return pending;
  }
}
