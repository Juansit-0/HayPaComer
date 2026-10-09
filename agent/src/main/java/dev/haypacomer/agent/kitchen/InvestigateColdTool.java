package dev.haypacomer.agent.kitchen;

import dev.haypacomer.agent.runtime.AgentTool;
import dev.haypacomer.agent.runtime.Observation;
import dev.haypacomer.agent.runtime.ToolInvocation;
import dev.haypacomer.agent.tools.ParameterSpec;
import dev.haypacomer.agent.tools.ParameterType;
import dev.haypacomer.agent.tools.ToolSpec;
import dev.haypacomer.application.coldchain.InvestigateColdIncidents;
import dev.haypacomer.domain.coldchain.investigation.ColdEpisode;
import dev.haypacomer.domain.coldchain.investigation.ColdInvestigation;
import dev.haypacomer.domain.coldchain.investigation.FoodVerdict;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class InvestigateColdTool implements AgentTool {

  public static final ToolSpec SPEC =
      ToolSpec.read(
          "investigate_cold",
          "Investigate warm periods in each fridge: when, how warm, how long, the likely cause,"
              + " and what to do with each food by the 2 hour rule.",
          List.of(
              ParameterSpec.optional(
                  "hours", ParameterType.COUNT, "Hours to look back, default 24")));

  private final InvestigateColdIncidents investigate;

  public InvestigateColdTool(InvestigateColdIncidents investigate) {
    this.investigate = investigate;
  }

  @Override
  public ToolSpec spec() {
    return SPEC;
  }

  @Override
  public String describe(ToolInvocation invocation) {
    return "Investigate cold incidents";
  }

  @Override
  public Observation invoke(ToolInvocation invocation) {
    int hours = Integer.parseInt(invocation.arguments().getOrDefault("hours", "24").strip());
    List<ColdInvestigation> found =
        investigate.investigate(
            invocation.user(), invocation.household(), Optional.empty(), Duration.ofHours(hours));
    if (found.isEmpty()) {
      return Observation.of(SPEC.name(), "No fridge to investigate");
    }
    return Observation.of(
        SPEC.name(),
        found.stream()
            .map(investigation -> describe(investigation, hours))
            .collect(Collectors.joining("\n")));
  }

  private static String describe(ColdInvestigation investigation, int hours) {
    if (investigation.readings() == 0) {
      return "No temperature readings in the last " + hours + " h";
    }
    if (investigation.episodes().isEmpty()) {
      return "Stayed at or below 5 C for the last "
          + hours
          + " h ("
          + investigation.readings()
          + " readings)";
    }
    String episodes =
        investigation.episodes().stream()
            .map(InvestigateColdTool::episode)
            .collect(Collectors.joining("; "));
    String foods =
        investigation.foods().stream()
            .filter(food -> food.verdict() != FoodVerdict.KEEP)
            .map(food -> food.food() + " " + food.verdict())
            .collect(Collectors.joining(", "));
    return "Warm periods: "
        + episodes
        + (foods.isEmpty() ? ". All food can stay." : ". Food: " + foods + ".");
  }

  private static String episode(ColdEpisode episode) {
    return "from "
        + episode.start()
        + episode.ended().map(end -> " to " + end).orElse(" (still warm)")
        + ", peak "
        + episode.peakCelsius().toPlainString()
        + " C, "
        + episode.aboveLimit().toMinutes()
        + " min above 5 C, likely "
        + episode.cause().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
  }
}
