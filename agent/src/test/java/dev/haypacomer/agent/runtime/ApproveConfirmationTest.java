package dev.haypacomer.agent.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.agent.confirm.ApproveConfirmation;
import dev.haypacomer.agent.confirm.ConfirmationRefusedException;
import dev.haypacomer.agent.tools.GuardrailChain;
import dev.haypacomer.agent.tools.ParameterSpec;
import dev.haypacomer.agent.tools.ParameterType;
import dev.haypacomer.agent.tools.PermissionGuardrail;
import dev.haypacomer.agent.tools.SchemaGuardrail;
import dev.haypacomer.agent.tools.ToolRegistry;
import dev.haypacomer.agent.tools.ToolSpec;
import dev.haypacomer.application.agent.AgentRun;
import dev.haypacomer.application.agent.ConfirmationDesk;
import dev.haypacomer.application.agent.PendingConfirmation;
import dev.haypacomer.application.agent.RunStatus;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ApproveConfirmationTest {

  private static final Instant NOW = Instant.parse("2026-10-09T18:00:00Z");

  private final InMemoryAgentStores stores = new InMemoryAgentStores();
  private final UserId owner = UserId.newId();
  private final Household home =
      Household.create("Home", Currency.getInstance("COP"), ZoneOffset.UTC, owner, NOW);
  private final SingleHousehold households = new SingleHousehold(home);
  private final FakeTool market =
      new FakeTool(
          ToolSpec.write(
              "add_to_market",
              "Market",
              Permission.MANAGE_MARKET_LIST,
              List.of(ParameterSpec.required("food", ParameterType.TEXT, "Food"))),
          "Added milk");
  private final FakeTool broken =
      new FakeTool(ToolSpec.write("notify_housemates", "x", Permission.COOK, List.of()), null);
  private final ToolRegistry tools = new ToolRegistry(List.of(market, broken));
  private final GuardrailChain guardrails =
      new GuardrailChain(List.of(new SchemaGuardrail(), new PermissionGuardrail(households)));
  private final ApproveConfirmation approve =
      new ApproveConfirmation(
          new ConfirmationDesk(stores, stores, Clock.fixed(NOW.plusSeconds(20), ZoneOffset.UTC)),
          tools,
          guardrails);

  private PendingConfirmation propose(UserId user, String tool, Map<String, String> arguments) {
    AgentRun run =
        AgentRun.start(home.id(), user, "market", 8, NOW)
            .advance(RunStatus.WAITING_CONFIRMATION, 1, null);
    stores.save(run);
    PendingConfirmation pending =
        PendingConfirmation.propose(run.id(), home.id(), user, tool, arguments, "Do it", NOW);
    stores.propose(pending);
    return pending;
  }

  @Test
  void approvingRunsTheRealToolOnceAndClosesTheRun() {
    PendingConfirmation pending = propose(owner, "add_to_market", Map.of("food", "milk"));

    Observation observation = approve.approve(owner, pending.id());

    assertEquals("Added milk", observation.content());
    assertEquals(1, market.invocations.size());
    assertEquals(RunStatus.DONE, stores.runs.get(pending.run()).status());
    assertEquals("Approved: Added milk", stores.traceOf(pending.run()).getLast().detail());
    assertTrue(stores.pending.isEmpty());
  }

  @Test
  void permissionsAreCheckedAgainWhenApproving() {
    UserId guest = UserId.newId();
    home.join(guest, Role.GUEST, NOW);
    PendingConfirmation pending = propose(guest, "add_to_market", Map.of("food", "milk"));

    assertThrows(ConfirmationRefusedException.class, () -> approve.approve(guest, pending.id()));
    assertTrue(market.invocations.isEmpty());
    assertEquals(RunStatus.FAILED, stores.runs.get(pending.run()).status());
    assertTrue(
        stores.traceOf(pending.run()).getLast().detail().contains("needs MANAGE_MARKET_LIST"));
  }

  @Test
  void removedToolsAndFailuresAreTraced() {
    PendingConfirmation gone = propose(owner, "create_label", Map.of());
    PendingConfirmation failing = propose(owner, "notify_housemates", Map.of());

    assertThrows(ConfirmationRefusedException.class, () -> approve.approve(owner, gone.id()));
    assertThrows(IllegalStateException.class, () -> approve.approve(owner, failing.id()));
    assertEquals(
        "Approved but failed: scale offline", stores.traceOf(failing.run()).getLast().detail());
    assertEquals(RunStatus.FAILED, stores.runs.get(failing.run()).status());
  }
}
