package dev.haypacomer.agent.memory;

import dev.haypacomer.agent.runtime.AgentTool;
import dev.haypacomer.agent.runtime.Observation;
import dev.haypacomer.agent.runtime.ToolInvocation;
import dev.haypacomer.agent.tools.ToolSpec;
import dev.haypacomer.application.agent.MemoryNote;
import dev.haypacomer.application.agent.ViewHouseholdMemory;
import java.util.List;
import java.util.stream.Collectors;

public final class RecallMemoryTool implements AgentTool {

  public static final ToolSpec SPEC =
      ToolSpec.read(
          "recall_memory",
          "Household preferences, usual quantities, accepted dishes, and decisions."
              + " Memory never replaces measured stock or safety rules.",
          List.of());

  private final ViewHouseholdMemory view;

  public RecallMemoryTool(ViewHouseholdMemory view) {
    this.view = view;
  }

  @Override
  public ToolSpec spec() {
    return SPEC;
  }

  @Override
  public String describe(ToolInvocation invocation) {
    return "Read household memory";
  }

  @Override
  public Observation invoke(ToolInvocation invocation) {
    List<MemoryNote> notes = view.view(invocation.user(), invocation.household());
    if (notes.isEmpty()) {
      return Observation.of(SPEC.name(), "Nothing remembered yet");
    }
    return Observation.of(
        SPEC.name(),
        notes.stream()
            .map(note -> note.topic() + " " + note.subject() + ": " + note.value())
            .collect(Collectors.joining("\n")));
  }
}
