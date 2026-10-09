package dev.haypacomer.agent.chat;

public final class ConversationNotFoundException extends RuntimeException {

  public ConversationNotFoundException() {
    super("Conversation not found");
  }
}
