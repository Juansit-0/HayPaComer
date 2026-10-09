package dev.haypacomer.agent.chat;

import dev.haypacomer.application.agent.ChatMessage;
import dev.haypacomer.application.agent.ConversationId;
import dev.haypacomer.application.port.ConversationStore;
import dev.haypacomer.domain.identity.UserId;
import java.util.List;

public final class ReadConversation {

  static final int LIMIT = 100;

  private final ConversationStore conversations;

  public ReadConversation(ConversationStore conversations) {
    this.conversations = conversations;
  }

  public List<ChatMessage> read(UserId user, ConversationId id) {
    boolean mine =
        conversations.recent(user, LIMIT).stream().anyMatch(summary -> summary.id().equals(id));
    if (!mine) {
      throw new ConversationNotFoundException();
    }
    return conversations.messages(id, LIMIT);
  }
}
