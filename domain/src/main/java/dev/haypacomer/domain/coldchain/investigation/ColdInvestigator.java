package dev.haypacomer.domain.coldchain.investigation;

import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.sensor.DoorEvent;
import dev.haypacomer.domain.sensor.DoorState;
import dev.haypacomer.domain.sensor.FridgeThresholds;
import dev.haypacomer.domain.sensor.SensorEvent;
import dev.haypacomer.domain.sensor.TemperatureReading;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ColdInvestigator {

  public static final Duration DOOR_LOOKBACK = Duration.ofMinutes(30);
  public static final Duration READING_GAP = Duration.ofMinutes(15);

  private final FridgeThresholds thresholds;
  private final ColdRule rule;

  public ColdInvestigator(FridgeThresholds thresholds) {
    this(thresholds, ColdRule.DEFAULT);
  }

  public ColdInvestigator(FridgeThresholds thresholds, ColdRule rule) {
    this.thresholds = thresholds;
    this.rule = rule;
  }

  public ColdInvestigation investigate(
      FridgeId fridge, Instant from, Instant to, List<SensorEvent> events, List<FoodItem> food) {
    if (!to.isAfter(from)) {
      throw new IllegalArgumentException("The investigation window ends before it starts");
    }
    List<TemperatureReading> readings =
        events.stream()
            .filter(TemperatureReading.class::isInstance)
            .map(TemperatureReading.class::cast)
            .sorted(Comparator.comparing(TemperatureReading::occurredAt))
            .toList();
    List<Instant[]> doorOpen = doorIntervals(events, to);
    List<ColdEpisode> episodes = episodes(readings, doorOpen, to);
    Duration total =
        episodes.stream().map(ColdEpisode::aboveLimit).reduce(Duration.ZERO, Duration::plus);
    List<FoodAssessment> assessments = food.stream().map(item -> assess(item, total)).toList();
    return new ColdInvestigation(fridge, from, to, readings.size(), episodes, total, assessments);
  }

  private List<ColdEpisode> episodes(
      List<TemperatureReading> readings, List<Instant[]> doorOpen, Instant to) {
    List<ColdEpisode> episodes = new ArrayList<>();
    Instant start = null;
    BigDecimal peak = null;
    Duration above = Duration.ZERO;
    boolean gap = false;
    for (int index = 0; index < readings.size(); index++) {
      TemperatureReading reading = readings.get(index);
      boolean warm = reading.celsius().compareTo(thresholds.maxCelsius()) > 0;
      if (start != null) {
        Duration step =
            Duration.between(readings.get(index - 1).occurredAt(), reading.occurredAt());
        above = above.plus(step);
        gap = gap || step.compareTo(READING_GAP) > 0;
        if (!warm) {
          episodes.add(episode(start, reading.occurredAt(), peak, above, gap, doorOpen));
          start = null;
          continue;
        }
        peak = peak.max(reading.celsius());
      } else if (warm) {
        start = reading.occurredAt();
        peak = reading.celsius();
        above = Duration.ZERO;
        gap = false;
      }
    }
    if (start != null) {
      Instant last = readings.getLast().occurredAt();
      Duration tail = Duration.between(last, to);
      episodes.add(
          episode(
              start,
              null,
              peak,
              above.plus(tail),
              gap || tail.compareTo(READING_GAP) > 0,
              doorOpen));
    }
    return episodes;
  }

  private ColdEpisode episode(
      Instant start,
      Instant end,
      BigDecimal peak,
      Duration above,
      boolean gap,
      List<Instant[]> doorOpen) {
    Instant windowStart = start.minus(DOOR_LOOKBACK);
    Instant windowEnd = end == null ? Instant.MAX : end;
    Duration open = Duration.ZERO;
    for (Instant[] interval : doorOpen) {
      Instant from = interval[0].isAfter(windowStart) ? interval[0] : windowStart;
      Instant to = interval[1].isBefore(windowEnd) ? interval[1] : windowEnd;
      if (to.isAfter(from)) {
        open = open.plus(Duration.between(from, to));
      }
    }
    LikelyCause cause =
        open.compareTo(thresholds.doorAlertAfter()) >= 0
            ? LikelyCause.DOOR_LEFT_OPEN
            : gap ? LikelyCause.READINGS_MISSING : LikelyCause.COOLING_OR_POWER;
    return new ColdEpisode(start, end, peak, above, open, cause);
  }

  private static List<Instant[]> doorIntervals(List<SensorEvent> events, Instant to) {
    List<DoorEvent> doors =
        events.stream()
            .filter(DoorEvent.class::isInstance)
            .map(DoorEvent.class::cast)
            .sorted(Comparator.comparing(DoorEvent::occurredAt))
            .toList();
    List<Instant[]> intervals = new ArrayList<>();
    Instant opened = null;
    for (DoorEvent door : doors) {
      if (door.state() == DoorState.OPEN && opened == null) {
        opened = door.occurredAt();
      } else if (door.state() == DoorState.CLOSED && opened != null) {
        intervals.add(new Instant[] {opened, door.occurredAt()});
        opened = null;
      }
    }
    if (opened != null) {
      intervals.add(new Instant[] {opened, to});
    }
    return intervals;
  }

  private FoodAssessment assess(FoodItem item, Duration above) {
    long minutes = above.toMinutes();
    String limit = "above " + thresholds.maxCelsius().stripTrailingZeros().toPlainString() + " C";
    if (!item.food().perishable()) {
      return new FoodAssessment(
          item.id(), item.name(), item.quantity(), FoodVerdict.KEEP, "Not perishable");
    }
    if (above.compareTo(rule.discardAfter()) >= 0) {
      return new FoodAssessment(
          item.id(),
          item.name(),
          item.quantity(),
          FoodVerdict.DISCARD,
          "Perishable and "
              + limit
              + " for "
              + minutes
              + " min in total ("
              + rule.discardAfter().toMinutes()
              + " min or more)");
    }
    if (above.compareTo(rule.useTodayAfter()) >= 0) {
      return new FoodAssessment(
          item.id(),
          item.name(),
          item.quantity(),
          FoodVerdict.USE_TODAY,
          "Perishable and " + limit + " for " + minutes + " min; use it today");
    }
    return new FoodAssessment(
        item.id(),
        item.name(),
        item.quantity(),
        FoodVerdict.KEEP,
        minutes == 0 ? "No time " + limit : "Only " + minutes + " min " + limit);
  }
}
