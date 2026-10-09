package dev.haypacomer.agent.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.agent.confirm.ApproveConfirmation;
import dev.haypacomer.agent.kitchen.AddToMarketTool;
import dev.haypacomer.agent.kitchen.KitchenFixture;
import dev.haypacomer.agent.kitchen.QueryInventoryTool;
import dev.haypacomer.agent.kitchen.ViewColdChainTool;
import dev.haypacomer.agent.kitchen.ViewExpiriesTool;
import dev.haypacomer.agent.kitchen.ViewMarketListTool;
import dev.haypacomer.agent.planning.LlmPlannerFactory;
import dev.haypacomer.agent.supervisor.KeywordRouter;
import dev.haypacomer.agent.supervisor.Supervisor;
import dev.haypacomer.agent.supervisor.SupervisorAnswer;
import dev.haypacomer.agent.tools.GuardrailChain;
import dev.haypacomer.agent.tools.PermissionGuardrail;
import dev.haypacomer.agent.tools.SchemaGuardrail;
import dev.haypacomer.agent.tools.ToolRegistry;
import dev.haypacomer.application.agent.AiOutcome;
import dev.haypacomer.application.agent.ChatModelUnavailableException;
import dev.haypacomer.application.agent.ConfirmationDesk;
import dev.haypacomer.application.agent.PendingConfirmation;
import dev.haypacomer.application.agent.RunStatus;
import dev.haypacomer.application.agent.TraceKind;
import dev.haypacomer.application.agent.TraceStep;
import dev.haypacomer.application.port.ChatModel;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AiAgentScenarioTest {

  private final KitchenFixture kitchen = new KitchenFixture();
  private final InMemoryAgentStores stores = new InMemoryAgentStores();
  private final Deque<String> answers = new ArrayDeque<>();
  private final ChatModel model =
      new ChatModel() {
        @Override
        public String name() {
          return "gemini";
        }

        @Override
        public String completeJson(String system, String user) {
          String next = answers.poll();
          if (next == null) {
            throw new ChatModelUnavailableException("quota exceeded");
          }
          return next;
        }
      };
  private final ToolRegistry tools =
      new ToolRegistry(
          List.of(
              new QueryInventoryTool(kitchen.inventory, kitchen.today),
              new ViewExpiriesTool(kitchen.inventory, kitchen.today),
              new ViewMarketListTool(kitchen.viewMarket()),
              new AddToMarketTool(kitchen.addToMarket(), kitchen.stores.catalog),
              new ViewColdChainTool(kitchen.coldChains())));
  private final GuardrailChain guardrails =
      new GuardrailChain(
          List.of(new SchemaGuardrail(), new PermissionGuardrail(kitchen.households)));
  private final Supervisor supervisor =
      new Supervisor(
          kitchen.households,
          tools,
          guardrails,
          new KeywordRouter(),
          new LlmPlannerFactory(model, tools),
          stores,
          stores,
          stores,
          kitchen.clock);

  {
    kitchen.put("Chicken breast", 650, KitchenFixture.TODAY.plusDays(1));
    kitchen.put("Rice", 900, null);
  }

  private SupervisorAnswer ask(String goal) {
    return supervisor.handle(kitchen.home.id(), kitchen.owner, goal, Optional.empty());
  }

  private List<TraceKind> trace(SupervisorAnswer answer) {
    return stores.traceOf(answer.parts().getFirst().run().id()).stream()
        .map(TraceStep::kind)
        .toList();
  }

  @Test
  void aGroundedModelAnswerKeepsTheMeasuredGrams() {
    answers.add("{\"action\":\"call\",\"tool\":\"view_expiries\",\"reason\":\"Rescue first\"}");
    answers.add(
        "{\"action\":\"answer\",\"text\":\"Cook the 650 g of chicken tonight with rice.\","
            + "\"evidence\":[\"view_expiries\"]}");

    SupervisorAnswer answer = ask("What can I cook tonight?");

    assertEquals("Cook the 650 g of chicken tonight with rice.", answer.answer());
    assertEquals(RunStatus.DONE, answer.parts().getFirst().run().status());
    assertTrue(stores.audit.stream().allMatch(entry -> entry.outcome() == AiOutcome.VALID));
    assertEquals("gemini", stores.audit.getFirst().provider());
    assertEquals(
        List.of(TraceKind.PLAN, TraceKind.TOOL_CALL, TraceKind.OBSERVATION, TraceKind.ANSWER),
        trace(answer));
  }

  @Test
  void anInventedAmountFallsBackToTheOfflineRules() {
    answers.add("{\"action\":\"call\",\"tool\":\"query_inventory\"}");
    answers.add("{\"action\":\"answer\",\"text\":\"Use 2000 g of chicken\"}");

    SupervisorAnswer answer = ask("What can I cook tonight?");

    assertTrue(answer.answer().startsWith("Offline answer based on measured data:"));
    assertTrue(answer.answer().contains("Chicken breast 650 g"));
    assertTrue(!answer.answer().contains("2000"));
    assertTrue(stores.audit.stream().anyMatch(entry -> entry.outcome() == AiOutcome.FALLBACK));
  }

  @Test
  void aProviderOutageStillAnswersFromMeasuredData() {
    SupervisorAnswer answer = ask("La nevera esta caliente?");

    assertEquals("cold", answer.parts().getFirst().run().specialist());
    assertTrue(answer.answer().startsWith("Offline answer based on measured data:"));
    assertTrue(answer.answer().contains("view_cold_chain:"));
    assertEquals(AiOutcome.FALLBACK, stores.audit.getFirst().outcome());
  }

  @Test
  void aModelCannotUseToolsOutsideItsSpecialist() {
    answers.add(
        "{\"action\":\"call\",\"tool\":\"add_to_market\",\"arguments\":{\"food\":\"Rice\","
            + "\"grams\":\"500\"}}");
    answers.add("{\"action\":\"answer\",\"text\":\"I cannot change the market list.\"}");

    SupervisorAnswer answer =
        supervisor.handle(
            kitchen.home.id(), kitchen.owner, "fridge temperature", Optional.of("cold"));

    assertEquals("I cannot change the market list.", answer.answer());
    assertTrue(stores.pending.isEmpty());
    assertTrue(kitchen.lists.isEmpty());
    assertTrue(stores.audit.stream().anyMatch(entry -> entry.outcome() == AiOutcome.REJECTED));
  }

  @Test
  void aModelWriteWaitsForAPersonAndThenRunsOnce() {
    answers.add("{\"action\":\"call\",\"tool\":\"view_market_list\"}");
    answers.add(
        "{\"action\":\"call\",\"tool\":\"add_to_market\",\"arguments\":{\"food\":\"Rice\","
            + "\"grams\":500},\"reason\":\"Rice will run out\"}");

    SupervisorAnswer answer = ask("What should I buy?");

    PendingConfirmation pending = answer.pending().orElseThrow();
    assertEquals("Add 500 g of Rice to the market list", pending.summary());
    assertTrue(kitchen.lists.isEmpty());
    ApproveConfirmation approve =
        new ApproveConfirmation(
            new ConfirmationDesk(stores, stores, kitchen.clock), tools, guardrails);
    assertEquals(
        "Added 500 g of Rice to the market list",
        approve.approve(kitchen.owner, pending.id()).content());
    assertEquals(1, kitchen.lists.get(kitchen.home.id()).pending().size());
    assertEquals(RunStatus.DONE, stores.runs.get(pending.run()).status());
  }
}
