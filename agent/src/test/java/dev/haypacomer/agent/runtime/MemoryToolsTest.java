package dev.haypacomer.agent.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.agent.memory.RecallMemoryTool;
import dev.haypacomer.agent.memory.RememberTool;
import dev.haypacomer.agent.tools.GuardrailChain;
import dev.haypacomer.agent.tools.PermissionGuardrail;
import dev.haypacomer.agent.tools.SchemaGuardrail;
import dev.haypacomer.agent.tools.ToolRegistry;
import dev.haypacomer.application.agent.PendingConfirmation;
import dev.haypacomer.application.agent.RememberForHousehold;
import dev.haypacomer.application.agent.RunStatus;
import dev.haypacomer.application.agent.ViewHouseholdMemory;
import dev.haypacomer.application.port.HouseholdMemory;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class MemoryToolsTest {

  private static final Instant START = Instant.parse("2026-10-09T18:00:00Z");

  private final Map<String, String> stored = new HashMap<>();
  private final HouseholdMemory memory =
      new HouseholdMemory() {
        @Override
        public Map<String, String> read(HouseholdId household) {
          return new TreeMap<>(stored);
        }

        @Override
        public void remember(HouseholdId household, String key, String value) {
          stored.put(key, value);
        }

        @Override
        public void forget(HouseholdId household, String key) {
          stored.remove(key);
        }
      };
  private final UserId owner = UserId.newId();
  private final Household home =
      Household.create("Home", Currency.getInstance("COP"), ZoneOffset.UTC, owner, START);
  private final SingleHousehold households = new SingleHousehold(home);
  private final RecallMemoryTool recall =
      new RecallMemoryTool(new ViewHouseholdMemory(households, memory));
  private final RememberTool remember =
      new RememberTool(new RememberForHousehold(households, memory));
  private final InMemoryAgentStores stores = new InMemoryAgentStores();
  private final AgentRuntime runtime =
      new AgentRuntime(
          new ToolRegistry(List.of(recall, remember)),
          new GuardrailChain(List.of(new SchemaGuardrail(), new PermissionGuardrail(households))),
          new ScriptedPlanner(),
          new RuleBasedPlanner(List.of("recall_memory")),
          stores,
          stores,
          stores,
          new SteppingClock(START, Duration.ofMillis(5)));
  private final AgentTask task = new AgentTask(home.id(), owner, "chef", "Plan dinner");

  private ToolInvocation call(Map<String, String> arguments) {
    return new ToolInvocation(home.id(), owner, arguments);
  }

  @Test
  void recallsWhatTheHouseholdTaughtTheAgent() {
    assertEquals("Nothing remembered yet", recall.invoke(call(Map.of())).content());
    stored.put("usual:rice", "150 g");
    stored.put("preference:spicy food", "mild");

    AgentResult result = runtime.run(task, AgentBudget.DEFAULT);

    assertEquals(
        "Offline answer based on measured data:\nrecall_memory: PREFERENCE spicy food: mild\n"
            + "USUAL_QUANTITY rice: 150 g",
        result.answerText().orElseThrow());
  }

  @Test
  void rememberingWaitsForConfirmationAndRunsTheUseCaseWhenInvoked() {
    ScriptedPlanner planner =
        new ScriptedPlanner(
            context ->
                new Decision.CallTool(
                    "remember",
                    Map.of("topic", "usual_quantity", "subject", "Rice", "value", "150 g"),
                    "They always cook 150 g"));
    AgentRuntime proposing =
        new AgentRuntime(
            new ToolRegistry(List.of(recall, remember)),
            new GuardrailChain(List.of(new SchemaGuardrail())),
            planner,
            new RuleBasedPlanner(List.of()),
            stores,
            stores,
            stores,
            new SteppingClock(START, Duration.ofMillis(5)));

    AgentResult result = proposing.run(task, AgentBudget.DEFAULT);

    assertEquals(RunStatus.WAITING_CONFIRMATION, result.run().status());
    PendingConfirmation pending = result.pending().orElseThrow();
    assertEquals("Remember USUAL_QUANTITY rice: 150 g", pending.summary());
    assertTrue(stored.isEmpty());
    assertEquals("Remembered usual:rice", remember.invoke(call(pending.arguments())).content());
    assertEquals(Map.of("usual:rice", "150 g"), stored);
  }

  @Test
  void invalidNotesAreRejectedBeforeAnyProposal() {
    ScriptedPlanner planner =
        new ScriptedPlanner(
            context ->
                new Decision.CallTool(
                    "remember",
                    Map.of("topic", "usual_quantity", "subject", "rice", "value", "plenty"),
                    "Rice"),
            context ->
                new Decision.CallTool(
                    "remember", Map.of("topic", "gossip", "subject", "x", "value", "y"), "x"),
            context -> new Decision.FinalAnswer("Nothing remembered"));
    AgentRuntime checking =
        new AgentRuntime(
            new ToolRegistry(List.of(remember)),
            new GuardrailChain(List.of(new SchemaGuardrail())),
            planner,
            new RuleBasedPlanner(List.of()),
            stores,
            stores,
            stores,
            new SteppingClock(START, Duration.ofMillis(5)));

    AgentResult result = checking.run(task, AgentBudget.DEFAULT);

    assertEquals(RunStatus.DONE, result.run().status());
    assertTrue(stores.pending.isEmpty());
    List<String> observations =
        planner.seen.getLast().history().stream()
            .map(exchange -> exchange.observation().content())
            .toList();
    assertTrue(observations.get(0).startsWith("Cannot remember this: "));
    assertEquals("Cannot remember this: unknown topic GOSSIP", observations.get(1));
  }
}
