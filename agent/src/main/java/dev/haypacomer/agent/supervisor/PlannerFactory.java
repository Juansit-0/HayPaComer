package dev.haypacomer.agent.supervisor;

import dev.haypacomer.agent.runtime.Planner;

public interface PlannerFactory {

  Planner plannerFor(Specialist specialist);
}
