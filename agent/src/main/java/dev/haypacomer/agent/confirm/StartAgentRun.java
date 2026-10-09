package dev.haypacomer.agent.confirm;

import dev.haypacomer.agent.runtime.AgentBudget;
import dev.haypacomer.agent.runtime.AgentResult;
import dev.haypacomer.agent.runtime.AgentRuntime;
import dev.haypacomer.agent.runtime.AgentTask;
import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.HouseholdRepository;

public final class StartAgentRun {

  private final GetHousehold households;
  private final AgentRuntime runtime;

  public StartAgentRun(HouseholdRepository households, AgentRuntime runtime) {
    this.households = new GetHousehold(households);
    this.runtime = runtime;
  }

  public AgentResult start(AgentTask task) {
    households.get(task.user(), task.household());
    return runtime.run(task, AgentBudget.DEFAULT);
  }
}
