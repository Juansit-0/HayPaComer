package dev.haypacomer.agent.confirm;

public final class ConfirmationRefusedException extends RuntimeException {

  public ConfirmationRefusedException(String reason) {
    super(reason);
  }
}
