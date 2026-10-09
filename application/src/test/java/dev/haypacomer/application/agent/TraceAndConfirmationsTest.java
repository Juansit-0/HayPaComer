package dev.haypacomer.application.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.identity.UserId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TraceAndConfirmationsTest {

  private static final Instant NOW = Instant.parse("2026-10-09T18:00:00Z");

  private final MemoryAgentStores stores = new MemoryAgentStores();
  private final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  private final UserId owner = UserId.newId();
  private final UserId housemate = UserId.newId();
  private final Household home =
      Household.create("Home", Currency.getInstance("COP"), ZoneOffset.UTC, owner, NOW);
  private final AgentRun run =
      AgentRun.start(home.id(), owner, "market", 8, NOW)
          .advance(RunStatus.WAITING_CONFIRMATION, 1, null);
  private final PendingConfirmation pending =
      PendingConfirmation.propose(
          run.id(), home.id(), owner, "add_to_market", Map.of("food", "milk"), "Add milk", NOW);

  {
    home.join(housemate, dev.haypacomer.domain.household.Role.MEMBER, NOW);
    households.save(home);
    stores.save(run);
    stores.trace(run.id(), new TraceStep(TraceKind.PLAN, "Milk is out", NOW));
    stores.propose(pending);
  }

  private ConfirmationDesk desk(Instant now) {
    return new ConfirmationDesk(stores, stores, Clock.fixed(now, ZoneOffset.UTC));
  }

  @Test
  void membersOfTheHouseholdReadTheTrace() {
    ViewAgentRun view = new ViewAgentRun(households, stores);

    RunTrace trace = view.view(housemate, run.id());

    assertEquals(run, trace.run());
    assertEquals(List.of(TraceKind.PLAN), trace.steps().stream().map(TraceStep::kind).toList());
    assertThrows(AgentRunNotFoundException.class, () -> view.view(UserId.newId(), run.id()));
    assertThrows(AgentRunNotFoundException.class, () -> view.view(owner, AgentRunId.newId()));
  }

  @Test
  void onlyTheProposerSeesLiveConfirmations() {
    PendingConfirmation old =
        PendingConfirmation.propose(
            run.id(), home.id(), owner, "remember", Map.of(), "Old", NOW.minusSeconds(900));
    stores.propose(old);
    ListPendingConfirmations list =
        new ListPendingConfirmations(stores, Clock.fixed(NOW, ZoneOffset.UTC));

    assertEquals(List.of(pending), list.list(owner));
    assertTrue(list.list(housemate).isEmpty());
  }

  @Test
  void rejectingClosesTheRunWithoutWriting() {
    RejectConfirmation reject = new RejectConfirmation(desk(NOW.plusSeconds(30)));

    assertThrows(ConfirmationNotFoundException.class, () -> reject.reject(housemate, pending.id()));
    PendingConfirmation rejected = reject.reject(owner, pending.id());

    assertEquals(pending, rejected);
    assertTrue(stores.pending.isEmpty());
    AgentRun closed = stores.runs.get(run.id());
    assertEquals(RunStatus.DONE, closed.status());
    assertEquals(NOW.plusSeconds(30), closed.finishedAt());
    assertEquals("Rejected by the person: Add milk", stores.traceOf(run.id()).getLast().detail());
    assertThrows(ConfirmationNotFoundException.class, () -> reject.reject(owner, pending.id()));
    assertThrows(
        ConfirmationNotFoundException.class, () -> reject.reject(owner, UUID.randomUUID()));
  }

  @Test
  void expiredConfirmationsFailTheRun() {
    ConfirmationDesk late = desk(NOW.plusSeconds(600));

    assertThrows(ConfirmationExpiredException.class, () -> late.take(owner, pending.id()));
    assertEquals(RunStatus.FAILED, stores.runs.get(run.id()).status());
    assertTrue(stores.pending.isEmpty());
  }

  @Test
  void closingAMissingRunStillLeavesATrace() {
    stores.runs.clear();

    desk(NOW).close(pending, RunStatus.DONE, "late");

    assertEquals("late", stores.traceOf(run.id()).getLast().detail());
    assertTrue(stores.runs.isEmpty());
  }
}
