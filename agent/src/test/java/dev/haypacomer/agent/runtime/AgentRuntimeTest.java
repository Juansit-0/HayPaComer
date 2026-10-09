package dev.haypacomer.agent.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.agent.AgentRun;
import dev.haypacomer.application.agent.AiOutcome;
import dev.haypacomer.application.agent.PendingConfirmation;
import dev.haypacomer.application.agent.RunStatus;
import dev.haypacomer.application.agent.TraceKind;
import dev.haypacomer.application.agent.TraceStep;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AgentRuntimeTest {

  private static final Instant START = Instant.parse("2026-10-09T18:00:00Z");

  private final InMemoryAgentStores stores = new InMemoryAgentStores();
  private final FakeTool inventory =
      new FakeTool("query_inventory", ToolKind.READ, "chicken 650 g, rice 900 g");
  private final FakeTool expiries =
      new FakeTool("view_expiries", ToolKind.READ, "chicken expires tomorrow");
  private final FakeTool weigh = new FakeTool("weigh_now", ToolKind.READ, null);
  private final FakeTool market = new FakeTool("add_to_market", ToolKind.WRITE, "added");
  private final AgentTask task =
      new AgentTask(HouseholdId.newId(), UserId.newId(), "chef", "What can I cook tonight?");
  private final RuleBasedPlanner offline =
      new RuleBasedPlanner(List.of("view_expiries", "query_inventory"));

  private AgentRuntime runtime(Planner planner, Duration tick) {
    return new AgentRuntime(
        List.of(inventory, expiries, weigh, market),
        planner,
        offline,
        stores,
        stores,
        stores,
        new SteppingClock(START, tick));
  }

  private AgentRuntime runtime(Planner planner) {
    return runtime(planner, Duration.ofMillis(10));
  }

  private static Decision call(String tool) {
    return new Decision.CallTool(tool, Map.of(), "Need " + tool);
  }

  private List<TraceKind> kinds(AgentRun run) {
    return stores.traceOf(run.id()).stream().map(TraceStep::kind).toList();
  }

  @Test
  void plansCallsObservesAndAnswers() {
    ScriptedPlanner planner =
        new ScriptedPlanner(
            context -> call("query_inventory"),
            context ->
                new Decision.FinalAnswer(
                    "Rice with chicken: " + context.history().getFirst().observation().content()));

    AgentResult result = runtime(planner).run(task, AgentBudget.DEFAULT);

    assertEquals(RunStatus.DONE, result.run().status());
    assertEquals(1, result.run().stepsUsed());
    assertTrue(result.run().finished().isPresent());
    assertEquals("Rice with chicken: chicken 650 g, rice 900 g", result.answerText().orElseThrow());
    assertTrue(result.pending().isEmpty());
    assertEquals(
        List.of(TraceKind.PLAN, TraceKind.TOOL_CALL, TraceKind.OBSERVATION, TraceKind.ANSWER),
        kinds(result.run()));
    assertEquals(result.run(), stores.find(result.run().id()).orElseThrow());
    assertEquals(task.household(), inventory.invocations.getFirst().household());
    assertEquals(7, planner.seen.get(1).stepsLeft());
    assertTrue(stores.audit.stream().allMatch(entry -> entry.outcome() == AiOutcome.VALID));
    assertEquals(Duration.ofMillis(10), stores.audit.getFirst().latency());
  }

  @Test
  void stopsWhenTheStepBudgetIsUsed() {
    ScriptedPlanner planner =
        new ScriptedPlanner(
            context -> call("query_inventory"),
            context -> call("view_expiries"),
            context -> call("query_inventory"));

    AgentResult result = runtime(planner).run(task, new AgentBudget(2, Duration.ofSeconds(30)));

    assertEquals(RunStatus.OUT_OF_BUDGET, result.run().status());
    assertEquals(2, result.run().stepsUsed());
    assertEquals(0, planner.seen.getLast().stepsLeft());
    assertTrue(result.answerText().isEmpty());
    assertTrue(stores.traceOf(result.run().id()).getLast().detail().contains("budget of 2"));
  }

  @Test
  void stopsWhenTheRunTakesTooLong() {
    ScriptedPlanner planner =
        new ScriptedPlanner(
            context -> call("query_inventory"),
            context -> call("view_expiries"),
            context -> call("query_inventory"));

    AgentResult result =
        runtime(planner, Duration.ofSeconds(4))
            .run(task, new AgentBudget(8, Duration.ofSeconds(10)));

    assertEquals(RunStatus.OUT_OF_BUDGET, result.run().status());
    assertTrue(stores.traceOf(result.run().id()).getLast().detail().contains("longer than"));
  }

  @Test
  void writesWaitForAHumanAndAreNeverInvoked() {
    ScriptedPlanner planner =
        new ScriptedPlanner(
            context ->
                new Decision.CallTool("add_to_market", Map.of("food", "milk"), "Milk is out"));

    AgentResult result = runtime(planner).run(task, AgentBudget.DEFAULT);

    assertEquals(RunStatus.WAITING_CONFIRMATION, result.run().status());
    assertTrue(result.run().finished().isEmpty());
    assertTrue(market.invocations.isEmpty());
    PendingConfirmation pending = result.pending().orElseThrow();
    assertEquals("add_to_market", pending.tool());
    assertEquals(Map.of("food", "milk"), pending.arguments());
    assertEquals("add_to_market {food=milk}", pending.summary());
    assertEquals(List.of(pending), stores.pendingFor(task.user()));
    assertEquals(TraceKind.OBSERVATION, kinds(result.run()).getLast());
  }

  @Test
  void unknownToolsAndFailuresBecomeObservations() {
    ScriptedPlanner planner =
        new ScriptedPlanner(
            context -> call("drop_table"),
            context -> call("weigh_now"),
            context -> new Decision.FinalAnswer("I could not read the scale"));

    AgentResult result = runtime(planner).run(task, AgentBudget.DEFAULT);

    assertEquals(RunStatus.DONE, result.run().status());
    assertEquals(2, result.run().stepsUsed());
    List<Exchange> history = planner.seen.getLast().history();
    assertEquals("Unknown tool", history.getFirst().observation().content());
    assertTrue(history.getFirst().observation().failed());
    assertEquals("Tool failed: scale offline", history.get(1).observation().content());
    assertTrue(stores.audit.stream().anyMatch(entry -> entry.outcome() == AiOutcome.REJECTED));
  }

  @Test
  void fallsBackToOfflineRulesWhenThePlannerIsUnavailable() {
    ScriptedPlanner planner = new ScriptedPlanner();

    AgentResult result = runtime(planner).run(task, AgentBudget.DEFAULT);

    assertEquals(RunStatus.DONE, result.run().status());
    assertEquals(2, result.run().stepsUsed());
    assertEquals(
        "Offline answer based on measured data:\n"
            + "view_expiries: chicken expires tomorrow\n"
            + "query_inventory: chicken 650 g, rice 900 g",
        result.answerText().orElseThrow());
    assertEquals(AiOutcome.FALLBACK, stores.audit.getFirst().outcome());
    assertEquals("scripted", stores.audit.getFirst().provider());
    assertEquals("offline", stores.audit.getLast().provider());
  }

  @Test
  void failsWhenNoPlannerIsAvailable() {
    Planner broken =
        new Planner() {
          @Override
          public String name() {
            return "broken";
          }

          @Override
          public Decision next(AgentContext context) {
            throw new PlannerUnavailableException("down");
          }
        };
    AgentRuntime runtime =
        new AgentRuntime(
            List.of(inventory),
            broken,
            broken,
            stores,
            stores,
            stores,
            new SteppingClock(START, Duration.ofMillis(10)));

    AgentResult result = runtime.run(task, AgentBudget.DEFAULT);

    assertEquals(RunStatus.FAILED, result.run().status());
    assertEquals(0, result.run().stepsUsed());
  }

  @Test
  void validatesItsInputs() {
    assertThrows(IllegalArgumentException.class, () -> new AgentBudget(0, Duration.ofSeconds(1)));
    assertThrows(IllegalArgumentException.class, () -> new AgentBudget(21, Duration.ofSeconds(1)));
    assertThrows(IllegalArgumentException.class, () -> new AgentBudget(3, Duration.ZERO));
    assertThrows(
        IllegalArgumentException.class,
        () -> new AgentTask(task.household(), task.user(), "chef", " "));
    assertThrows(IllegalArgumentException.class, () -> new Decision.FinalAnswer(""));
    assertEquals(Observation.MAX_LENGTH, Observation.of("t", "x".repeat(5000)).content().length());
    assertFalse(Observation.of("t", "ok").failed());
  }
}
