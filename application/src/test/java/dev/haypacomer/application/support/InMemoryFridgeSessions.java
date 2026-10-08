package dev.haypacomer.application.support;

import dev.haypacomer.application.port.FridgeSessionRegistry;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.fridge.FridgeSession;
import java.util.HashMap;
import java.util.Map;

public final class InMemoryFridgeSessions implements FridgeSessionRegistry {

  private final Map<FridgeId, FridgeSession> sessions = new HashMap<>();

  @Override
  public FridgeSession sessionOf(FridgeId fridge) {
    return sessions.computeIfAbsent(fridge, FridgeSession::new);
  }
}
