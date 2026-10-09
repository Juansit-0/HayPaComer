package dev.haypacomer.agent.supervisor;

import dev.haypacomer.agent.runtime.AgentBudget;
import dev.haypacomer.agent.runtime.AgentResult;
import dev.haypacomer.agent.runtime.AgentRuntime;
import dev.haypacomer.agent.runtime.AgentTask;
import dev.haypacomer.agent.runtime.RuleBasedPlanner;
import dev.haypacomer.agent.tools.GuardrailChain;
import dev.haypacomer.agent.tools.ToolRegistry;
import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.AgentRunStore;
import dev.haypacomer.application.port.AiAuditLog;
import dev.haypacomer.application.port.ConfirmationStore;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class Supervisor {

  private final GetHousehold households;
  private final ToolRegistry tools;
  private final GuardrailChain guardrails;
  private final SpecialistRouter router;
  private final PlannerFactory planners;
  private final AgentRunStore runs;
  private final ConfirmationStore confirmations;
  private final AiAuditLog audit;
  private final Clock clock;

  public Supervisor(
      HouseholdRepository households,
      ToolRegistry tools,
      GuardrailChain guardrails,
      SpecialistRouter router,
      PlannerFactory planners,
      AgentRunStore runs,
      ConfirmationStore confirmations,
      AiAuditLog audit,
      Clock clock) {
    this.households = new GetHousehold(households);
    this.tools = tools;
    this.guardrails = guardrails;
    this.router = router;
    this.planners = planners;
    this.runs = runs;
    this.confirmations = confirmations;
    this.audit = audit;
    this.clock = clock;
  }

  public SupervisorAnswer handle(
      HouseholdId household, UserId user, String goal, Optional<String> specialist) {
    households.get(user, household);
    List<Specialist> chosen =
        specialist
            .map(
                name ->
                    List.of(
                        Specialist.ALL.stream()
                            .filter(candidate -> candidate.name().equals(name))
                            .findFirst()
                            .orElseThrow(
                                () -> new IllegalArgumentException("Unknown specialist " + name))))
            .orElseGet(() -> router.route(goal));
    List<AgentResult> parts = new ArrayList<>();
    for (Specialist current : chosen) {
      AgentRuntime runtime =
          new AgentRuntime(
              tools.allow(
                  current.tools().stream()
                      .filter(tools.names()::contains)
                      .collect(Collectors.toSet())),
              guardrails,
              planners.plannerFor(current),
              new RuleBasedPlanner(current.offlineReads()),
              runs,
              confirmations,
              audit,
              clock);
      AgentResult result =
          runtime.run(new AgentTask(household, user, current.name(), goal), AgentBudget.DEFAULT);
      parts.add(result);
      if (result.pending().isPresent()) {
        break;
      }
    }
    return new SupervisorAnswer(parts, merge(parts));
  }

  private static String merge(List<AgentResult> parts) {
    if (parts.size() == 1) {
      return text(parts.getFirst());
    }
    return parts.stream()
        .map(part -> capitalize(part.run().specialist()) + ": " + text(part))
        .collect(Collectors.joining("\n\n"));
  }

  private static String text(AgentResult part) {
    return part.answerText()
        .or(
            () ->
                part.pending()
                    .map(pending -> "Waiting for your confirmation: " + pending.summary()))
        .orElse("Could not finish (" + part.run().status() + ")");
  }

  private static String capitalize(String name) {
    return Character.toUpperCase(name.charAt(0)) + name.substring(1);
  }
}
