package dev.haypacomer.agent.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.agent.kitchen.KitchenFixture;
import dev.haypacomer.agent.kitchen.QueryInventoryTool;
import dev.haypacomer.agent.kitchen.ViewExpiriesTool;
import dev.haypacomer.agent.runtime.AgentContext;
import dev.haypacomer.agent.runtime.Decision;
import dev.haypacomer.agent.runtime.Planner;
import dev.haypacomer.agent.supervisor.KeywordRouter;
import dev.haypacomer.agent.supervisor.Supervisor;
import dev.haypacomer.agent.tools.GuardrailChain;
import dev.haypacomer.agent.tools.PermissionGuardrail;
import dev.haypacomer.agent.tools.SchemaGuardrail;
import dev.haypacomer.agent.tools.ToolRegistry;
import dev.haypacomer.application.agent.AgentRun;
import dev.haypacomer.application.agent.AgentRunId;
import dev.haypacomer.application.agent.AiAuditEntry;
import dev.haypacomer.application.agent.ChatMessage;
import dev.haypacomer.application.agent.ChatRole;
import dev.haypacomer.application.agent.ConversationId;
import dev.haypacomer.application.agent.ConversationSummary;
import dev.haypacomer.application.agent.PendingConfirmation;
import dev.haypacomer.application.agent.TraceStep;
import dev.haypacomer.application.port.AgentRunStore;
import dev.haypacomer.application.port.AiAuditLog;
import dev.haypacomer.application.port.ConfirmationStore;
import dev.haypacomer.application.port.ConversationStore;
import dev.haypacomer.domain.identity.UserId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChefChatTest {

  private final KitchenFixture kitchen = new KitchenFixture();
  private final Map<ConversationId, List<ChatMessage>> stored = new LinkedHashMap<>();
  private final Map<UserId, List<ConversationId>> owners = new HashMap<>();
  private final ConversationStore conversations =
      new ConversationStore() {
        @Override
        public void append(UserId user, ConversationId conversation, ChatMessage message) {
          stored.computeIfAbsent(conversation, id -> new ArrayList<>()).add(message);
          List<ConversationId> mine = owners.computeIfAbsent(user, id -> new ArrayList<>());
          if (!mine.contains(conversation)) {
            mine.add(conversation);
          }
        }

        @Override
        public List<ChatMessage> messages(ConversationId conversation, int limit) {
          List<ChatMessage> all = stored.getOrDefault(conversation, List.of());
          return all.subList(Math.max(0, all.size() - limit), all.size());
        }

        @Override
        public List<ConversationSummary> recent(UserId user, int limit) {
          return owners.getOrDefault(user, List.of()).stream()
              .map(id -> new ConversationSummary(id, KitchenFixture.NOW))
              .toList();
        }
      };
  private final List<String> goals = new ArrayList<>();
  private final Planner chef =
      new Planner() {
        @Override
        public String name() {
          return "scripted";
        }

        @Override
        public Decision next(AgentContext context) {
          goals.add(context.task().goal());
          return context.history().isEmpty()
              ? new Decision.CallTool("query_inventory", Map.of(), "Read stock")
              : new Decision.FinalAnswer("Cook rice with the 650 g of chicken");
        }
      };
  private final ChefChat chat = new ChefChat(supervisor(), conversations, kitchen.clock);

  private Supervisor supervisor() {
    AgentRunStore runs =
        new AgentRunStore() {
          @Override
          public void save(AgentRun run) {}

          @Override
          public Optional<AgentRun> find(AgentRunId id) {
            return Optional.empty();
          }

          @Override
          public void trace(AgentRunId id, TraceStep step) {}

          @Override
          public List<TraceStep> traceOf(AgentRunId id) {
            return List.of();
          }
        };
    ConfirmationStore confirmations =
        new ConfirmationStore() {
          @Override
          public void propose(PendingConfirmation confirmation) {}

          @Override
          public Optional<PendingConfirmation> find(UUID id) {
            return Optional.empty();
          }

          @Override
          public List<PendingConfirmation> pendingFor(UserId user) {
            return List.of();
          }

          @Override
          public void remove(PendingConfirmation confirmation) {}
        };
    AiAuditLog audit =
        new AiAuditLog() {
          @Override
          public void record(AiAuditEntry entry) {}

          @Override
          public List<AiAuditEntry> recent(int limit) {
            return List.of();
          }
        };
    ToolRegistry tools =
        new ToolRegistry(
            List.of(
                new QueryInventoryTool(kitchen.inventory, kitchen.today),
                new ViewExpiriesTool(kitchen.inventory, kitchen.today)));
    return new Supervisor(
        kitchen.households,
        tools,
        new GuardrailChain(
            List.of(new SchemaGuardrail(), new PermissionGuardrail(kitchen.households))),
        new KeywordRouter(),
        specialist -> chef,
        runs,
        confirmations,
        audit,
        kitchen.clock);
  }

  @Test
  void answersWithEvidenceAndKeepsTheConversation() {
    kitchen.put("Chicken breast", 650, KitchenFixture.TODAY.plusDays(1));

    ChatReply first =
        chat.chat(
            kitchen.home.id(),
            kitchen.owner,
            Optional.empty(),
            " What can I cook? ",
            Optional.empty());

    assertEquals("Cook rice with the 650 g of chicken", first.answer().answer());
    assertEquals("query_inventory", first.evidence().getFirst().tool());
    assertTrue(first.evidence().getFirst().content().contains("Chicken breast 650 g"));
    assertEquals(
        List.of(ChatRole.USER, ChatRole.ASSISTANT),
        stored.get(first.conversation()).stream().map(ChatMessage::role).toList());
    assertEquals("What can I cook?", goals.getFirst());

    ChatReply second =
        chat.chat(
            kitchen.home.id(),
            kitchen.owner,
            Optional.of(first.conversation()),
            "And for tomorrow?",
            Optional.of("chef"));

    assertEquals(first.conversation(), second.conversation());
    assertEquals(4, stored.get(first.conversation()).size());
    assertTrue(goals.getLast().startsWith("And for tomorrow?\n\nEarlier in this conversation:\n"));
    assertTrue(goals.getLast().contains("user: What can I cook?"));
    assertTrue(goals.getLast().contains("assistant: Cook rice"));
    assertEquals(
        4, new ReadConversation(conversations).read(kitchen.owner, first.conversation()).size());
  }

  @Test
  void conversationsBelongToTheirAuthor() {
    ChatReply mine =
        chat.chat(kitchen.home.id(), kitchen.owner, Optional.empty(), "Hola", Optional.empty());

    assertThrows(
        ConversationNotFoundException.class,
        () ->
            chat.chat(
                kitchen.home.id(),
                kitchen.guest,
                Optional.of(mine.conversation()),
                "Peek",
                Optional.empty()));
    assertThrows(
        ConversationNotFoundException.class,
        () -> new ReadConversation(conversations).read(kitchen.guest, mine.conversation()));
    assertThrows(
        IllegalArgumentException.class,
        () -> chat.chat(kitchen.home.id(), kitchen.owner, Optional.empty(), " ", Optional.empty()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            chat.chat(
                kitchen.home.id(),
                kitchen.owner,
                Optional.empty(),
                "x".repeat(1001),
                Optional.empty()));
  }
}
