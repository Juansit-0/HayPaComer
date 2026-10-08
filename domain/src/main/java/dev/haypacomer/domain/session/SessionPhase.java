package dev.haypacomer.domain.session;

public enum SessionPhase {
  PREPARING,
  COOKING,
  PAUSED,
  FINISHED,
  ABANDONED;

  public boolean active() {
    return this != FINISHED && this != ABANDONED;
  }
}
