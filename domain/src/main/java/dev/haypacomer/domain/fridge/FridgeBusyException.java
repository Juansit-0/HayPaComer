package dev.haypacomer.domain.fridge;

public final class FridgeBusyException extends RuntimeException {

  public FridgeBusyException() {
    super("Someone else is using the fridge right now");
  }
}
