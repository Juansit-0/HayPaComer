package dev.haypacomer.agent.kitchen;

import dev.haypacomer.agent.runtime.AgentTool;
import dev.haypacomer.agent.runtime.Observation;
import dev.haypacomer.agent.runtime.ToolInvocation;
import dev.haypacomer.agent.tools.ToolSpec;
import dev.haypacomer.application.coldchain.ListColdChains;
import dev.haypacomer.domain.coldchain.ColdChain;
import java.util.List;
import java.util.stream.Collectors;

public final class ViewColdChainTool implements AgentTool {

  public static final ToolSpec SPEC =
      ToolSpec.read(
          "view_cold_chain",
          "Cold chain of every fridge: phase, last measured temperature, pending review.",
          List.of());

  private final ListColdChains chains;

  public ViewColdChainTool(ListColdChains chains) {
    this.chains = chains;
  }

  @Override
  public ToolSpec spec() {
    return SPEC;
  }

  @Override
  public String describe(ToolInvocation invocation) {
    return "Read the cold chain";
  }

  @Override
  public Observation invoke(ToolInvocation invocation) {
    List<ColdChain> found = chains.list(invocation.user(), invocation.household());
    if (found.isEmpty()) {
      return Observation.of(SPEC.name(), "No temperature readings yet");
    }
    return Observation.of(
        SPEC.name(), found.stream().map(ViewColdChainTool::line).collect(Collectors.joining("\n")));
  }

  private static String line(ColdChain chain) {
    String reading =
        chain
            .lastCelsius()
            .map(
                celsius -> celsius.toPlainString() + " C at " + chain.lastReadingAt().orElseThrow())
            .orElse("no reading");
    return "Fridge "
        + chain.fridge().value()
        + ": "
        + chain.phase()
        + ", "
        + reading
        + (chain.needsReview() ? ", needs human review" : "");
  }
}
