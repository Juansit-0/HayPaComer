package dev.haypacomer.application.session;

import dev.haypacomer.domain.session.CookingSessionId;

public final class SessionAlreadyActiveException extends RuntimeException {

  private final CookingSessionId active;

  public SessionAlreadyActiveException(CookingSessionId active) {
    super("The household is already cooking in session " + active.value());
    this.active = active;
  }

  public CookingSessionId active() {
    return active;
  }
}
