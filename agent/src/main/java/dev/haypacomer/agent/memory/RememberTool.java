package dev.haypacomer.agent.memory;

import dev.haypacomer.agent.runtime.AgentTool;
import dev.haypacomer.agent.runtime.Observation;
import dev.haypacomer.agent.runtime.ToolInvocation;
import dev.haypacomer.agent.tools.ParameterSpec;
import dev.haypacomer.agent.tools.ParameterType;
import dev.haypacomer.agent.tools.ToolSpec;
import dev.haypacomer.application.agent.MemoryNote;
import dev.haypacomer.application.agent.MemoryTopic;
import dev.haypacomer.application.agent.RememberForHousehold;
import dev.haypacomer.domain.household.Permission;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class RememberTool implements AgentTool {

  public static final ToolSpec SPEC =
      ToolSpec.write(
          "remember",
          "Propose a note for household memory after the person confirms it.",
          Permission.COOK,
          List.of(
              ParameterSpec.required(
                  "topic",
                  ParameterType.TEXT,
                  "preference, usual_quantity, accepted_dish, or decision"),
              ParameterSpec.required("subject", ParameterType.TEXT, "What the note is about"),
              ParameterSpec.required("value", ParameterType.TEXT, "What to remember")));

  private final RememberForHousehold remember;

  public RememberTool(RememberForHousehold remember) {
    this.remember = remember;
  }

  @Override
  public ToolSpec spec() {
    return SPEC;
  }

  @Override
  public Optional<String> problem(ToolInvocation invocation) {
    try {
      note(invocation);
      return Optional.empty();
    } catch (RuntimeException invalid) {
      return Optional.of("Cannot remember this: " + invalid.getMessage());
    }
  }

  @Override
  public String describe(ToolInvocation invocation) {
    MemoryNote note = note(invocation);
    return "Remember " + note.topic() + " " + note.subject() + ": " + note.value();
  }

  @Override
  public Observation invoke(ToolInvocation invocation) {
    MemoryNote note =
        remember.remember(invocation.user(), invocation.household(), note(invocation));
    return Observation.of(SPEC.name(), "Remembered " + note.key());
  }

  private static MemoryNote note(ToolInvocation invocation) {
    String topic = invocation.arguments().get("topic").strip().toUpperCase(Locale.ROOT);
    MemoryTopic parsed =
        Arrays.stream(MemoryTopic.values())
            .filter(candidate -> candidate.name().equals(topic))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("unknown topic " + topic));
    return new MemoryNote(
        parsed, invocation.arguments().get("subject"), invocation.arguments().get("value"));
  }
}
