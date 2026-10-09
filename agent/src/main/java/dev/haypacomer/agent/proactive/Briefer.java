package dev.haypacomer.agent.proactive;

import dev.haypacomer.agent.supervisor.Supervisor;
import dev.haypacomer.application.notification.Notification;
import dev.haypacomer.application.notification.NotificationType;
import dev.haypacomer.application.notification.NotifyHousehold;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.household.HouseholdId;
import java.time.Clock;
import java.util.Optional;

public final class Briefer {

  static final int MAX_BODY = 1500;

  private final HouseholdRepository households;
  private final Supervisor supervisor;
  private final NotifyHousehold delivery;
  private final Clock clock;

  public Briefer(
      HouseholdRepository households,
      Supervisor supervisor,
      NotifyHousehold delivery,
      Clock clock) {
    this.households = households;
    this.supervisor = supervisor;
    this.delivery = delivery;
    this.clock = clock;
  }

  public Notification publish(HouseholdId household, String title, String body) {
    Notification digest =
        Notification.of(
            household,
            NotificationType.BRIEFING,
            title,
            body.length() > MAX_BODY ? body.substring(0, MAX_BODY) : body,
            clock.instant());
    delivery.publish(digest);
    return digest;
  }

  public Optional<Notification> brief(
      HouseholdId household, String specialist, String goal, String title) {
    return households
        .findById(household)
        .map(
            found -> {
              String answer =
                  supervisor
                      .handle(household, found.owner(), goal, Optional.of(specialist))
                      .answer();
              Notification briefing =
                  Notification.of(
                      household,
                      NotificationType.BRIEFING,
                      title,
                      answer.length() > MAX_BODY ? answer.substring(0, MAX_BODY) : answer,
                      clock.instant());
              delivery.publish(briefing);
              return briefing;
            });
  }
}
