package dev.haypacomer.application.agent;

import dev.haypacomer.application.port.AgentRunStore;
import dev.haypacomer.application.port.ConfirmationStore;
import dev.haypacomer.domain.identity.UserId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class MemoryAgentStores implements AgentRunStore, ConfirmationStore {

  final Map<AgentRunId, AgentRun> runs = new HashMap<>();
  final Map<AgentRunId, List<TraceStep>> traces = new HashMap<>();
  final Map<UUID, PendingConfirmation> pending = new HashMap<>();

  @Override
  public void save(AgentRun run) {
    runs.put(run.id(), run);
  }

  @Override
  public Optional<AgentRun> find(AgentRunId id) {
    return Optional.ofNullable(runs.get(id));
  }

  @Override
  public void trace(AgentRunId id, TraceStep step) {
    traces.computeIfAbsent(id, key -> new ArrayList<>()).add(step);
  }

  @Override
  public List<TraceStep> traceOf(AgentRunId id) {
    return List.copyOf(traces.getOrDefault(id, List.of()));
  }

  @Override
  public void propose(PendingConfirmation confirmation) {
    pending.put(confirmation.id(), confirmation);
  }

  @Override
  public Optional<PendingConfirmation> find(UUID id) {
    return Optional.ofNullable(pending.get(id));
  }

  @Override
  public List<PendingConfirmation> pendingFor(UserId user) {
    return pending.values().stream().filter(p -> p.user().equals(user)).toList();
  }

  @Override
  public void remove(PendingConfirmation confirmation) {
    pending.remove(confirmation.id());
  }
}
