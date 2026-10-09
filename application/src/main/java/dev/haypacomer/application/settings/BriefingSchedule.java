package dev.haypacomer.application.settings;

public record BriefingSchedule(int morningHour, int digestHour, int expiryClusterSize) {

  public static final BriefingSchedule DEFAULT = new BriefingSchedule(7, 8, 3);

  public BriefingSchedule {
    if (morningHour < 0 || morningHour > 23 || digestHour < 0 || digestHour > 23) {
      throw new IllegalArgumentException("Briefing hours go from 0 to 23");
    }
    if (expiryClusterSize < 2) {
      throw new IllegalArgumentException("An expiry cluster has at least 2 foods");
    }
  }
}
