package dev.haypacomer.application.session;

import dev.haypacomer.domain.session.CookingSessionId;

public final class CookingSessionNotFoundException extends RuntimeException {

  public CookingSessionNotFoundException(CookingSessionId id) {
    super("Cooking session not found: " + id.value());
  }
}
