package dev.haypacomer.application.market;

public final class MarketBudgetNotSetException extends RuntimeException {

  public MarketBudgetNotSetException() {
    super("No monthly market budget yet");
  }
}
