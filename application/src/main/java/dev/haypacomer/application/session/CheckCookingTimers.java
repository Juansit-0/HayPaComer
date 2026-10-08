package dev.haypacomer.application.session;

import java.time.Clock;
import java.util.Objects;

public final class CheckCookingTimers {

  private final KitchenMediator mediator;
  private final Clock clock;

  public CheckCookingTimers(KitchenMediator mediator, Clock clock) {
    this.mediator = Objects.requireNonNull(mediator, "mediator");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public void check() {
    mediator.notify(new KitchenEvent.ClockTicked(clock.instant()));
  }
}
