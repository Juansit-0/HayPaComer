package dev.haypacomer.application.notification;

import dev.haypacomer.domain.household.HouseholdId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Notification(
    UUID id, HouseholdId household, NotificationType type, String title, String body, Instant at) {

  public Notification {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(household, "household");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(body, "body");
    Objects.requireNonNull(at, "at");
  }

  public static Notification of(
      HouseholdId household, NotificationType type, String title, String body, Instant at) {
    return new Notification(UUID.randomUUID(), household, type, title, body, at);
  }
}
