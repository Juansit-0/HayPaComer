package dev.haypacomer.agent.tools;

import dev.haypacomer.agent.runtime.ToolInvocation;
import dev.haypacomer.application.port.HouseholdRepository;
import java.util.Optional;

public final class PermissionGuardrail implements Guardrail {

  private final HouseholdRepository households;

  public PermissionGuardrail(HouseholdRepository households) {
    this.households = households;
  }

  @Override
  public Optional<String> reject(ToolSpec spec, ToolInvocation invocation) {
    return households
        .findById(invocation.household())
        .filter(household -> household.membershipOf(invocation.user()).isPresent())
        .map(
            household ->
                household.can(invocation.user(), spec.permission())
                    ? Optional.<String>empty()
                    : Optional.of("Not allowed: " + spec.name() + " needs " + spec.permission()))
        .orElse(Optional.of("Not a member of this household"));
  }
}
