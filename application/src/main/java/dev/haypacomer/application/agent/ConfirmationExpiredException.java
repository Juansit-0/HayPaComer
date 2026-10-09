package dev.haypacomer.application.agent;

public final class ConfirmationExpiredException extends RuntimeException {

  public ConfirmationExpiredException() {
    super("The confirmation expired; ask the agent again");
  }
}
