package dev.haypacomer.application.agent;

public final class ConfirmationNotFoundException extends RuntimeException {

  public ConfirmationNotFoundException() {
    super("Confirmation not found");
  }
}
