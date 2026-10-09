package dev.haypacomer.agent.supervisor;

import dev.haypacomer.agent.runtime.Planner;
import dev.haypacomer.agent.runtime.RuleBasedPlanner;

public final class OfflinePlanners implements PlannerFactory {

  @Override
  public Planner plannerFor(Specialist specialist) {
    return new RuleBasedPlanner(specialist.offlineReads());
  }
}
