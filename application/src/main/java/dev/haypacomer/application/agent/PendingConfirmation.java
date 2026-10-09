package dev.haypacomer.application.agent;

import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record PendingConfirmation(
    UUID id,
    AgentRunId run,
    HouseholdId household,
    UserId user,
    String tool,
    Map<String, String> arguments,
    String summary,
    Instant proposedAt,
    Instant expiresAt) {

  public static final Duration TIME_TO_LIVE = Duration.ofMinutes(10);

  public PendingConfirmation {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(run, "run");
    Objects.requireNonNull(household, "household");
    Objects.requireNonNull(user, "user");
    Objects.requireNonNull(tool, "tool");
    Objects.requireNonNull(summary, "summary");
    Objects.requireNonNull(proposedAt, "proposedAt");
    Objects.requireNonNull(expiresAt, "expiresAt");
    arguments = Map.copyOf(arguments);
    if (!expiresAt.isAfter(proposedAt)) {
      throw new IllegalArgumentException("A confirmation must expire after it is proposed");
    }
  }

  public static PendingConfirmation propose(
      AgentRunId run,
      HouseholdId household,
      UserId user,
      String tool,
      Map<String, String> arguments,
      String summary,
      Instant at) {
    return new PendingConfirmation(
        UUID.randomUUID(),
        run,
        household,
        user,
        tool,
        arguments,
        summary,
        at,
        at.plus(TIME_TO_LIVE));
  }

  public boolean expired(Instant now) {
    return !now.isBefore(expiresAt);
  }
}
