package dev.haypacomer.domain.planning;

import java.util.List;

public interface PlanningStrategy {

  List<PlanEntry> plan(PlanningContext context);
}
