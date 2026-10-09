package dev.haypacomer.agent.planning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.agent.kitchen.KitchenFixture;
import dev.haypacomer.agent.kitchen.QueryInventoryTool;
import dev.haypacomer.agent.kitchen.ViewExpiriesTool;
import dev.haypacomer.agent.runtime.AgentContext;
import dev.haypacomer.agent.runtime.AgentTask;
import dev.haypacomer.agent.runtime.Decision;
import dev.haypacomer.agent.runtime.Exchange;
import dev.haypacomer.agent.runtime.Observation;
import dev.haypacomer.agent.runtime.PlannerUnavailableException;
import dev.haypacomer.agent.supervisor.Specialist;
import dev.haypacomer.agent.tools.ToolRegistry;
import dev.haypacomer.application.agent.ChatModelUnavailableException;
import dev.haypacomer.application.port.ChatModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LlmPlannerTest {

  private final KitchenFixture kitchen = new KitchenFixture();
  private final ToolRegistry tools =
      new ToolRegistry(
          List.of(
              new QueryInventoryTool(kitchen.inventory, kitchen.today),
              new ViewExpiriesTool(kitchen.inventory, kitchen.today)));
  private final AgentTask task =
      new AgentTask(kitchen.home.id(), kitchen.owner, "chef", "Que cocino hoy?");
  private final List<String> systems = new ArrayList<>();
  private final List<String> users = new ArrayList<>();

  private LlmPlanner planner(String answer) {
    return new LlmPlanner(model(answer), Specialist.CHEF, tools);
  }

  private ChatModel model(String answer) {
    return new ChatModel() {
      @Override
      public String name() {
        return "gemini";
      }

      @Override
      public String completeJson(String system, String user) {
        systems.add(system);
        users.add(user);
        if (answer == null) {
          throw new ChatModelUnavailableException("down");
        }
        return answer;
      }
    };
  }

  private AgentContext fresh() {
    return new AgentContext(task, Set.of("query_inventory", "view_expiries"), List.of(), 8);
  }

  private AgentContext afterReading(String observed) {
    Decision.CallTool call = new Decision.CallTool("query_inventory", Map.of(), "read");
    return new AgentContext(
        task,
        Set.of("query_inventory", "view_expiries"),
        List.of(
            new Exchange(call, Observation.of("query_inventory", observed)),
            new Exchange(
                new Decision.CallTool("view_expiries", Map.of("days", "2"), "read"),
                Observation.failure("view_expiries", "down"))),
        6);
  }

  @Test
  void asksTheModelWithToolsAndHistoryAndReadsACall() {
    Decision decision =
        planner(
                "{\"action\":\"call\",\"tool\":\"view_expiries\",\"arguments\":{\"days\":2},"
                    + "\"reason\":\"Check what expires\"}")
            .next(afterReading("Chicken breast 650 g"));

    assertEquals(
        new Decision.CallTool("view_expiries", Map.of("days", "2"), "Check what expires"),
        decision);
    assertTrue(systems.getFirst().contains("- query_inventory (read): Measured food"));
    assertTrue(systems.getFirst().contains("Arguments: days count optional"));
    assertTrue(users.getFirst().contains("Goal: Que cocino hoy?"));
    assertTrue(users.getFirst().contains("and observed: Chicken breast 650 g"));
    assertTrue(users.getFirst().contains("and it failed: down"));
    assertEquals("gemini", planner("{}").name());
  }

  @Test
  void callsWithoutReasonOrArgumentsAreAccepted() {
    assertEquals(
        new Decision.CallTool("query_inventory", Map.of(), "Model chose query_inventory"),
        planner("{\"action\":\"call\",\"tool\":\"query_inventory\"}").next(fresh()));
  }

  @Test
  void answersMustQuoteOnlyObservedGramsAndCitedTools() {
    AgentContext read = afterReading("Chicken breast 650 g, Rice 900 g");

    assertEquals(
        new Decision.FinalAnswer("Usa 650 g de pollo y 900 gramos de arroz."),
        planner(
                "{\"action\":\"answer\",\"text\":\"Usa 650 g de pollo y 900 gramos de arroz.\","
                    + "\"evidence\":[\"query_inventory\"]}")
            .next(read));
    assertThrows(
        PlannerUnavailableException.class,
        () -> planner("{\"action\":\"answer\",\"text\":\"Use 200 g of chicken\"}").next(read));
    assertThrows(
        PlannerUnavailableException.class,
        () -> planner("{\"action\":\"answer\",\"text\":\"Use 65 g\"}").next(read));
    assertThrows(
        PlannerUnavailableException.class,
        () ->
            planner("{\"action\":\"answer\",\"text\":\"Cook rice\",\"evidence\":[\"weigh_now\"]}")
                .next(read));
    assertThrows(
        PlannerUnavailableException.class,
        () -> planner("{\"action\":\"answer\",\"text\":\"Use 650 g\"}").next(fresh()));
  }

  @Test
  void anythingElseMakesThePlannerUnavailable() {
    for (String answer :
        List.of(
            "not json",
            "[]",
            "{\"action\":\"dance\"}",
            "{\"action\":\"call\"}",
            "{\"action\":\"call\",\"tool\":\"" + "x".repeat(41) + "\"}",
            "{\"action\":\"call\",\"tool\":\"query_inventory\",\"arguments\":{\"food\":{\"a\":1}}}",
            "{\"action\":\"call\",\"tool\":\"query_inventory\",\"arguments\":{\"food\":null}}",
            "{\"action\":\"answer\",\"text\":\" \"}",
            "{\"action\":\"answer\",\"text\":\"" + "a".repeat(1501) + "\"}")) {
      assertThrows(PlannerUnavailableException.class, () -> planner(answer).next(fresh()), answer);
    }
    assertThrows(PlannerUnavailableException.class, () -> planner(null).next(fresh()));
  }

  @Test
  void longReasonsAndObservationsAreClipped() {
    Decision decision =
        planner(
                "{\"action\":\"call\",\"tool\":\"query_inventory\",\"reason\":\""
                    + "r".repeat(300)
                    + "\"}")
            .next(afterReading("x".repeat(3000)));

    assertEquals(200, ((Decision.CallTool) decision).reason().length());
    assertTrue(users.getFirst().length() < 3000);
    assertTrue(
        new LlmPlannerFactory(model("{}"), tools).plannerFor(Specialist.COLD)
            instanceof LlmPlanner);
  }
}
