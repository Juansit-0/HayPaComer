package dev.haypacomer.ai.resilience;

import dev.haypacomer.application.agent.ChatModelUnavailableException;
import dev.haypacomer.application.port.ChatModel;
import java.util.Objects;

public final class ResilientChatModel implements ChatModel {

  private final ChatModel provider;
  private final ChatModel fallback;
  private final ProviderCircuit circuit;

  public ResilientChatModel(ChatModel provider, ChatModel fallback, ProviderCircuit circuit) {
    this.provider = Objects.requireNonNull(provider, "provider");
    this.fallback = Objects.requireNonNull(fallback, "fallback");
    this.circuit = Objects.requireNonNull(circuit, "circuit");
  }

  @Override
  public String name() {
    return provider.name();
  }

  @Override
  public String completeJson(String system, String user) {
    return circuit.call(
        () -> provider.completeJson(system, user),
        () -> fallback.completeJson(system, user),
        failure -> failure instanceof ChatModelUnavailableException);
  }
}
