package dev.haypacomer.agent.chat;

import dev.haypacomer.agent.supervisor.Supervisor;
import dev.haypacomer.agent.supervisor.SupervisorAnswer;
import dev.haypacomer.application.agent.ChatMessage;
import dev.haypacomer.application.agent.ChatRole;
import dev.haypacomer.application.agent.ConversationId;
import dev.haypacomer.application.port.ConversationStore;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ChefChat {

  static final int CONTEXT_MESSAGES = 6;
  static final int MAX_MESSAGE = 1000;

  private final Supervisor supervisor;
  private final ConversationStore conversations;
  private final Clock clock;

  public ChefChat(Supervisor supervisor, ConversationStore conversations, Clock clock) {
    this.supervisor = supervisor;
    this.conversations = conversations;
    this.clock = clock;
  }

  public ChatReply chat(
      HouseholdId household,
      UserId user,
      Optional<ConversationId> existing,
      String message,
      Optional<String> specialist) {
    String text = message.strip();
    if (text.isEmpty() || text.length() > MAX_MESSAGE) {
      throw new IllegalArgumentException("A message has 1 to " + MAX_MESSAGE + " characters");
    }
    ConversationId conversation =
        existing.map(id -> owned(user, id)).orElseGet(ConversationId::newId);
    List<ChatMessage> earlier =
        existing.isPresent() ? conversations.messages(conversation, CONTEXT_MESSAGES) : List.of();
    SupervisorAnswer answer = supervisor.handle(household, user, goal(earlier, text), specialist);
    conversations.append(user, conversation, new ChatMessage(ChatRole.USER, text, clock.instant()));
    conversations.append(
        user,
        conversation,
        new ChatMessage(ChatRole.ASSISTANT, clip(answer.answer()), clock.instant()));
    return new ChatReply(conversation, answer);
  }

  private ConversationId owned(UserId user, ConversationId id) {
    return conversations.recent(user, 100).stream()
        .filter(summary -> summary.id().equals(id))
        .findFirst()
        .orElseThrow(ConversationNotFoundException::new)
        .id();
  }

  private static String goal(List<ChatMessage> earlier, String text) {
    if (earlier.isEmpty()) {
      return text;
    }
    String context =
        earlier.stream()
            .map(
                message ->
                    message.role().name().toLowerCase(java.util.Locale.ROOT)
                        + ": "
                        + clip(message.text(), 300))
            .collect(Collectors.joining("\n"));
    return text + "\n\nEarlier in this conversation:\n" + context;
  }

  private static String clip(String text) {
    return clip(text, ChatMessage.MAX_LENGTH);
  }

  private static String clip(String text, int max) {
    return text.length() > max ? text.substring(0, max) : text;
  }
}
