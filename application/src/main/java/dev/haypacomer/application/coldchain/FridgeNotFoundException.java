package dev.haypacomer.application.coldchain;

public final class FridgeNotFoundException extends RuntimeException {

  public FridgeNotFoundException() {
    super("Fridge not found");
  }
}
