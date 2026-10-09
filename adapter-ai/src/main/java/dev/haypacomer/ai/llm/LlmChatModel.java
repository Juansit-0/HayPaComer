package dev.haypacomer.ai.llm;

import dev.haypacomer.application.agent.ChatModelUnavailableException;
import dev.haypacomer.application.port.ChatModel;
import java.util.Locale;

public final class LlmChatModel implements ChatModel {

  private final LlmClient client;

  public LlmChatModel(LlmClient client) {
    this.client = client;
  }

  @Override
  public String name() {
    return client.source().name().toLowerCase(Locale.ROOT);
  }

  @Override
  public String completeJson(String system, String user) {
    try {
      return client.completeJson(new LlmPrompt(system, user));
    } catch (AiUnavailableException | InvalidAiResponseException failure) {
      throw new ChatModelUnavailableException(failure.getMessage());
    }
  }
}
