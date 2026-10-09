package dev.haypacomer.ai.resilience;

import dev.haypacomer.application.port.ChatModel;

public final class NullChatModel implements ChatModel {

  public static final String DEFER =
      "{\"action\":\"defer\",\"reason\":\"No AI provider is answering right now\"}";

  @Override
  public String name() {
    return "none";
  }

  @Override
  public String completeJson(String system, String user) {
    return DEFER;
  }
}
