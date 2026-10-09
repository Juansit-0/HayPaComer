package dev.haypacomer.agent.supervisor;

import java.util.List;

public interface SpecialistRouter {

  List<Specialist> route(String goal);
}
