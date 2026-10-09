package dev.haypacomer.application.agent;

public final class AgentRunNotFoundException extends RuntimeException {

  public AgentRunNotFoundException() {
    super("Agent run not found");
  }
}
