package dev.haypacomer.application.port;

import dev.haypacomer.application.agent.AgentRun;
import dev.haypacomer.application.agent.AgentRunId;
import dev.haypacomer.application.agent.TraceStep;
import java.util.List;
import java.util.Optional;

public interface AgentRunStore {

  void save(AgentRun run);

  Optional<AgentRun> find(AgentRunId id);

  void trace(AgentRunId id, TraceStep step);

  List<TraceStep> traceOf(AgentRunId id);
}
