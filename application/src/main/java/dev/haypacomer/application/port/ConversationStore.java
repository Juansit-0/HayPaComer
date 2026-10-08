package dev.haypacomer.application.port;

import dev.haypacomer.application.agent.ChatMessage;
import dev.haypacomer.application.agent.ConversationId;
import dev.haypacomer.application.agent.ConversationSummary;
import dev.haypacomer.domain.identity.UserId;
import java.util.List;

public interface ConversationStore {

  void append(UserId user, ConversationId conversation, ChatMessage message);

  List<ChatMessage> messages(ConversationId conversation, int limit);

  List<ConversationSummary> recent(UserId user, int limit);
}
