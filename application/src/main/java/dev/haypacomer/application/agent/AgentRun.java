package dev.haypacomer.application.agent;

import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record AgentRun(
    AgentRunId id,
    HouseholdId household,
    UserId user,
    String specialist,
    RunStatus status,
    int stepsUsed,
    int stepBudget,
    Instant startedAt,
    Instant finishedAt) {

  public AgentRun {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(household, "household");
    Objects.requireNonNull(user, "user");
    Objects.requireNonNull(specialist, "specialist");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(startedAt, "startedAt");
    if (stepBudget < 1 || stepsUsed < 0 || stepsUsed > stepBudget) {
      throw new IllegalArgumentException("Steps used must stay within the budget");
    }
  }

  public static AgentRun start(
      HouseholdId household, UserId user, String specialist, int stepBudget, Instant at) {
    return new AgentRun(
        AgentRunId.newId(),
        household,
        user,
        specialist,
        RunStatus.RUNNING,
        0,
        stepBudget,
        at,
        null);
  }

  public AgentRun advance(RunStatus newStatus, int newStepsUsed, Instant newFinishedAt) {
    return new AgentRun(
        id,
        household,
        user,
        specialist,
        newStatus,
        newStepsUsed,
        stepBudget,
        startedAt,
        newFinishedAt);
  }

  public Optional<Instant> finished() {
    return Optional.ofNullable(finishedAt);
  }
}
