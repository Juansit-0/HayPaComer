package dev.haypacomer.application.agent;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.household.HouseholdNotFoundException;
import dev.haypacomer.application.port.AgentRunStore;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.identity.UserId;
import java.util.Objects;

public final class ViewAgentRun {

  private final GetHousehold households;
  private final AgentRunStore runs;

  public ViewAgentRun(HouseholdRepository households, AgentRunStore runs) {
    this.households = new GetHousehold(households);
    this.runs = Objects.requireNonNull(runs, "runs");
  }

  public RunTrace view(UserId actor, AgentRunId id) {
    AgentRun run = runs.find(id).orElseThrow(AgentRunNotFoundException::new);
    try {
      households.get(actor, run.household());
    } catch (HouseholdNotFoundException notMember) {
      throw new AgentRunNotFoundException();
    }
    return new RunTrace(run, runs.traceOf(id));
  }
}
