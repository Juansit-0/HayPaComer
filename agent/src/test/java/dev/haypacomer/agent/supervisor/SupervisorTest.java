package dev.haypacomer.agent.supervisor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.agent.kitchen.AddToMarketTool;
import dev.haypacomer.agent.kitchen.KitchenFixture;
import dev.haypacomer.agent.kitchen.QueryInventoryTool;
import dev.haypacomer.agent.kitchen.ViewColdChainTool;
import dev.haypacomer.agent.kitchen.ViewExpiriesTool;
import dev.haypacomer.agent.kitchen.ViewMarketListTool;
import dev.haypacomer.agent.kitchen.ViewWeeklyPlanTool;
import dev.haypacomer.agent.memory.RecallMemoryTool;
import dev.haypacomer.agent.memory.RememberTool;
import dev.haypacomer.agent.runtime.AgentContext;
import dev.haypacomer.agent.runtime.AgentResult;
import dev.haypacomer.agent.runtime.Decision;
import dev.haypacomer.agent.runtime.Planner;
import dev.haypacomer.agent.tools.GuardrailChain;
import dev.haypacomer.agent.tools.PermissionGuardrail;
import dev.haypacomer.agent.tools.SchemaGuardrail;
import dev.haypacomer.agent.tools.ToolRegistry;
import dev.haypacomer.application.agent.AgentRun;
import dev.haypacomer.application.agent.AgentRunId;
import dev.haypacomer.application.agent.AiAuditEntry;
import dev.haypacomer.application.agent.PendingConfirmation;
import dev.haypacomer.application.agent.RememberForHousehold;
import dev.haypacomer.application.agent.RunStatus;
import dev.haypacomer.application.agent.TraceStep;
import dev.haypacomer.application.agent.ViewHouseholdMemory;
import dev.haypacomer.application.household.HouseholdNotFoundException;
import dev.haypacomer.application.port.AgentRunStore;
import dev.haypacomer.application.port.AiAuditLog;
import dev.haypacomer.application.port.ConfirmationStore;
import dev.haypacomer.application.port.HouseholdMemory;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SupervisorTest {

  private final KitchenFixture kitchen = new KitchenFixture();
  private final Map<AgentRunId, AgentRun> runs = new HashMap<>();
  private final List<PendingConfirmation> proposed = new ArrayList<>();
  private final Map<String, String> notes = new TreeMap<>();
  private final AgentRunStore runStore =
      new AgentRunStore() {
        @Override
        public void save(AgentRun run) {
          runs.put(run.id(), run);
        }

        @Override
        public Optional<AgentRun> find(AgentRunId id) {
          return Optional.ofNullable(runs.get(id));
        }

        @Override
        public void trace(AgentRunId id, TraceStep step) {}

        @Override
        public List<TraceStep> traceOf(AgentRunId id) {
          return List.of();
        }
      };
  private final ConfirmationStore confirmations =
      new ConfirmationStore() {
        @Override
        public void propose(PendingConfirmation confirmation) {
          proposed.add(confirmation);
        }

        @Override
        public Optional<PendingConfirmation> find(UUID id) {
          return Optional.empty();
        }

        @Override
        public List<PendingConfirmation> pendingFor(UserId user) {
          return proposed;
        }

        @Override
        public void remove(PendingConfirmation confirmation) {}
      };
  private final AiAuditLog audit =
      new AiAuditLog() {
        @Override
        public void record(AiAuditEntry entry) {}

        @Override
        public List<AiAuditEntry> recent(int limit) {
          return List.of();
        }
      };
  private final HouseholdMemory memory =
      new HouseholdMemory() {
        @Override
        public Map<String, String> read(HouseholdId household) {
          return notes;
        }

        @Override
        public void remember(HouseholdId household, String key, String value) {
          notes.put(key, value);
        }

        @Override
        public void forget(HouseholdId household, String key) {
          notes.remove(key);
        }
      };
  private final ToolRegistry tools =
      new ToolRegistry(
          List.of(
              new RecallMemoryTool(new ViewHouseholdMemory(kitchen.households, memory)),
              new RememberTool(new RememberForHousehold(kitchen.households, memory)),
              new QueryInventoryTool(kitchen.inventory, kitchen.today),
              new ViewExpiriesTool(kitchen.inventory, kitchen.today),
              new ViewMarketListTool(kitchen.viewMarket()),
              new AddToMarketTool(kitchen.addToMarket(), kitchen.stores.catalog),
              new ViewColdChainTool(kitchen.coldChains()),
              new ViewWeeklyPlanTool(kitchen.currentPlan())));
  private final GuardrailChain guardrails =
      new GuardrailChain(
          List.of(new SchemaGuardrail(), new PermissionGuardrail(kitchen.households)));

  private Supervisor supervisor(PlannerFactory planners) {
    return new Supervisor(
        kitchen.households,
        tools,
        guardrails,
        new KeywordRouter(),
        planners,
        runStore,
        confirmations,
        audit,
        kitchen.clock);
  }

  @Test
  void routesByKeywordsInEnglishAndSpanish() {
    KeywordRouter router = new KeywordRouter();

    assertEquals(List.of(Specialist.CHEF), router.route("What can I cook tonight?"));
    assertEquals(List.of(Specialist.MARKET), router.route("Qué toca comprar en el mercado"));
    assertEquals(List.of(Specialist.COLD), router.route("La nevera está caliente"));
    assertEquals(List.of(Specialist.COACH), router.route("How do I waste less?"));
    assertEquals(
        List.of(Specialist.MARKET, Specialist.CHEF), router.route("What do I buy to cook dinner?"));
    assertEquals(
        List.of(Specialist.COLD, Specialist.MARKET),
        router.route("door open, temperature high, buy ice, cook, waste"));
    assertEquals(List.of(Specialist.CHEF), router.route("Hola"));
    assertEquals(List.of(Specialist.MARKET), router.route("Que compro?"));
  }

  @Test
  void specialistsAnswerWithTheirOwnToolsAndMergeEvidence() {
    kitchen.put("Chicken breast", 650, KitchenFixture.TODAY.plusDays(1));
    notes.put("usual:rice", "150 g");

    SupervisorAnswer answer =
        supervisor(new OfflinePlanners())
            .handle(
                kitchen.home.id(),
                kitchen.owner,
                "What do I buy to cook dinner?",
                Optional.empty());

    assertEquals(2, answer.parts().size());
    assertEquals(
        List.of("market", "chef"),
        answer.parts().stream().map(part -> part.run().specialist()).toList());
    assertTrue(answer.answer().startsWith("Market: Offline answer based on measured data:"));
    assertTrue(answer.answer().contains("view_market_list: The market list is empty"));
    assertTrue(answer.answer().contains("Chef: Offline answer based on measured data:"));
    assertTrue(answer.answer().contains("view_expiries: Chicken breast 650 g"));
    assertTrue(answer.answer().contains("recall_memory: USUAL_QUANTITY rice: 150 g"));
    assertTrue(answer.pending().isEmpty());
    assertTrue(answer.parts().stream().allMatch(part -> part.run().status() == RunStatus.DONE));
  }

  @Test
  void aSpecialistCanBeChosenAndOnlySeesItsTools() {
    List<AgentContext> seen = new ArrayList<>();
    Planner recorder =
        new Planner() {
          @Override
          public String name() {
            return "recorder";
          }

          @Override
          public Decision next(AgentContext context) {
            seen.add(context);
            return new Decision.FinalAnswer("Fridge looks fine");
          }
        };

    SupervisorAnswer answer =
        supervisor(specialist -> recorder)
            .handle(kitchen.home.id(), kitchen.owner, "anything", Optional.of("cold"));

    assertEquals("Fridge looks fine", answer.answer());
    assertEquals(java.util.Set.of("view_cold_chain", "view_expiries"), seen.getFirst().tools());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            supervisor(new OfflinePlanners())
                .handle(kitchen.home.id(), kitchen.owner, "x", Optional.of("pirate")));
  }

  @Test
  void aProposedWriteStopsTheSupervisorUntilConfirmed() {
    kitchen.stores.catalog.save(KitchenFixture.food("Milk"));
    Planner market =
        new Planner() {
          @Override
          public String name() {
            return "llm";
          }

          @Override
          public Decision next(AgentContext context) {
            return new Decision.CallTool(
                "add_to_market", Map.of("food", "Milk", "grams", "1000"), "Milk is out");
          }
        };

    SupervisorAnswer answer =
        supervisor(specialist -> market)
            .handle(kitchen.home.id(), kitchen.owner, "Buy milk and cook dinner", Optional.empty());

    assertEquals(1, answer.parts().size());
    assertEquals(
        "Waiting for your confirmation: Add 1000 g of Milk to the market list", answer.answer());
    assertEquals(proposed.getFirst(), answer.pending().orElseThrow());
  }

  @Test
  void unfinishedSpecialistsAreReportedAndStrangersAreRejected() {
    Planner looping =
        new Planner() {
          @Override
          public String name() {
            return "loop";
          }

          @Override
          public Decision next(AgentContext context) {
            return new Decision.CallTool("view_expiries", Map.of(), "again");
          }
        };

    SupervisorAnswer answer =
        supervisor(specialist -> looping)
            .handle(kitchen.home.id(), kitchen.owner, "cook and buy", Optional.empty());

    assertEquals(
        "Market: Could not finish (OUT_OF_BUDGET)\n\nChef: Could not finish (OUT_OF_BUDGET)",
        answer.answer().replace("view_expiries", ""));
    assertThrows(
        HouseholdNotFoundException.class,
        () ->
            supervisor(new OfflinePlanners())
                .handle(kitchen.home.id(), UserId.newId(), "cook", Optional.empty()));
    AgentResult first = answer.parts().getFirst();
    assertEquals(RunStatus.OUT_OF_BUDGET, first.run().status());
  }

  @Test
  void specialistsRejectOfflineReadsOutsideTheirTools() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Specialist("x", "y", java.util.Set.of("a"), List.of("b")));
  }
}
