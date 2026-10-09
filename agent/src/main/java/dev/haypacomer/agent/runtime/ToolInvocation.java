package dev.haypacomer.agent.runtime;

import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.util.Map;
import java.util.Objects;

public record ToolInvocation(HouseholdId household, UserId user, Map<String, String> arguments) {

  public ToolInvocation {
    Objects.requireNonNull(household, "household");
    Objects.requireNonNull(user, "user");
    arguments = Map.copyOf(arguments);
  }
}
