package dev.haypacomer.domain.session;

public final class IllegalSessionTransitionException extends RuntimeException {

  public IllegalSessionTransitionException(SessionPhase phase, String action) {
    super("Cannot " + action + " a session that is " + phase);
  }
}
