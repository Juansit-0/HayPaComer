package dev.haypacomer.application.planning;

public final class WeeklyPlanNotFoundException extends RuntimeException {

  public WeeklyPlanNotFoundException() {
    super("Weekly plan not found");
  }
}
