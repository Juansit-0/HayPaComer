package dev.haypacomer.agent.runtime;

import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.util.Objects;

public record AgentTask(HouseholdId household, UserId user, String specialist, String goal) {

  public AgentTask {
    Objects.requireNonNull(household, "household");
    Objects.requireNonNull(user, "user");
    Objects.requireNonNull(specialist, "specialist");
    Objects.requireNonNull(goal, "goal");
    if (goal.isBlank()) {
      throw new IllegalArgumentException("A task needs a goal");
    }
  }
}
