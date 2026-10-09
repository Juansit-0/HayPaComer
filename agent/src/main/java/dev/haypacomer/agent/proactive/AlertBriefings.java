package dev.haypacomer.agent.proactive;

import dev.haypacomer.application.notification.Notification;
import dev.haypacomer.application.notification.NotificationType;
import dev.haypacomer.application.port.BriefingLog;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.NotificationListener;
import java.time.LocalDate;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

public final class AlertBriefings implements NotificationListener {

  private final HouseholdRepository households;
  private final BriefingLog log;
  private final Briefer briefer;
  private final Executor executor;
  private final AtomicInteger failures = new AtomicInteger();

  public AlertBriefings(
      HouseholdRepository households, BriefingLog log, Briefer briefer, Executor executor) {
    this.households = households;
    this.log = log;
    this.briefer = briefer;
    this.executor = executor;
  }

  @Override
  public void onNotification(Notification alert) {
    if (alert.type() == NotificationType.BRIEFING) {
      return;
    }
    executor.execute(() -> briefSafely(alert));
  }

  private void briefSafely(Notification alert) {
    try {
      households
          .findById(alert.household())
          .ifPresent(
              household -> {
                LocalDate day = LocalDate.ofInstant(alert.at(), household.timezone());
                String kind = "alert-" + alert.type().name().toLowerCase(Locale.ROOT);
                if (log.claim(alert.household(), kind, day)) {
                  briefer.brief(
                      alert.household(),
                      "cold",
                      "The fridge raised an alert: "
                          + alert.title()
                          + ". What should we check and use first?",
                      "What to check after: " + alert.title());
                }
              });
    } catch (RuntimeException unavailable) {
      failures.incrementAndGet();
    }
  }

  public int failures() {
    return failures.get();
  }
}
