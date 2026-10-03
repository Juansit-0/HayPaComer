package dev.haypacomer.application.household;

public final class HouseholdNotFoundException extends RuntimeException {

  public HouseholdNotFoundException() {
    super("Household not found");
  }
}
