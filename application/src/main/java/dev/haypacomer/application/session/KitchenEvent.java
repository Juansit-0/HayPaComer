package dev.haypacomer.application.session;

import dev.haypacomer.domain.session.CookingSession;
import java.time.Instant;
import java.util.Objects;

public sealed interface KitchenEvent {

  Instant at();

  record SessionChanged(CookingSession session, Instant at) implements KitchenEvent {

    public SessionChanged {
      Objects.requireNonNull(session, "session");
      Objects.requireNonNull(at, "at");
    }
  }

  record ClockTicked(Instant at) implements KitchenEvent {

    public ClockTicked {
      Objects.requireNonNull(at, "at");
    }
  }
}
