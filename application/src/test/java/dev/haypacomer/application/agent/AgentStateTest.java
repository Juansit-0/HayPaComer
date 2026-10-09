package dev.haypacomer.application.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AgentStateTest {

  private static final Instant NOW = Instant.parse("2026-10-08T23:00:00Z");

  @Test
  void runsStayWithinTheirStepBudget() {
    AgentRun run = AgentRun.start(HouseholdId.newId(), UserId.newId(), "chef", 8, NOW);

    assertEquals(RunStatus.RUNNING, run.status());
    assertEquals(0, run.stepsUsed());
    assertTrue(run.finished().isEmpty());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new AgentRun(
                run.id(), run.household(), run.user(), "chef", RunStatus.DONE, 9, 8, NOW, NOW));
    assertThrows(
        IllegalArgumentException.class,
        () -> AgentRun.start(run.household(), run.user(), "chef", 0, NOW));
  }

  @Test
  void confirmationsExpireAfterTenMinutes() {
    PendingConfirmation pending =
        PendingConfirmation.propose(
            AgentRunId.newId(),
            HouseholdId.newId(),
            UserId.newId(),
            "add_to_market",
            Map.of("food", "Rice", "grams", "500"),
            "Add 500 g of rice to the market list",
            NOW);

    assertEquals(NOW.plus(PendingConfirmation.TIME_TO_LIVE), pending.expiresAt());
    assertFalse(pending.expired(NOW.plusSeconds(599)));
    assertTrue(pending.expired(NOW.plusSeconds(600)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PendingConfirmation(
                pending.id(),
                pending.run(),
                pending.household(),
                pending.user(),
                "add_to_market",
                Map.of(),
                "x",
                NOW,
                NOW));
  }

  @Test
  void messagesAndAuditEntriesAreValidated() {
    assertThrows(IllegalArgumentException.class, () -> new ChatMessage(ChatRole.USER, " ", NOW));
    assertThrows(
        IllegalArgumentException.class,
        () -> new ChatMessage(ChatRole.USER, "x".repeat(ChatMessage.MAX_LENGTH + 1), NOW));
    assertEquals("hi", new ChatMessage(ChatRole.ASSISTANT, "hi", NOW).text());
    assertThrows(
        IllegalArgumentException.class,
        () -> new AiAuditEntry("gemini", "suggest", Duration.ofMillis(-1), AiOutcome.VALID, NOW));
    assertEquals(
        AiOutcome.FALLBACK,
        new AiAuditEntry("gemini", "suggest", Duration.ofMillis(5), AiOutcome.FALLBACK, NOW)
            .outcome());
    assertEquals(
        TraceKind.TOOL_CALL, new TraceStep(TraceKind.TOOL_CALL, "query_inventory", NOW).kind());
    ConversationId id = ConversationId.newId();
    assertEquals(id, new ConversationSummary(id, NOW).id());
  }
}
