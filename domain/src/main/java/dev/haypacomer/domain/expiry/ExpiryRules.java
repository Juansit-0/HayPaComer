package dev.haypacomer.domain.expiry;

import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.ZoneKind;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public record ExpiryRules(int marginDays) {

  public static final ExpiryRules DEFAULT = new ExpiryRules(3);

  public ExpiryRules {
    if (marginDays < 0) {
      throw new IllegalArgumentException("Margin days cannot be negative: " + marginDays);
    }
  }

  public ExpiryEstimate estimate(ShelfLife life, ZoneKind zone, boolean opened, LocalDate today) {
    int days = life.daysIn(zone, opened);
    return new ExpiryEstimate(today.plusDays(days), ExpirySource.ESTIMATED, days);
  }

  public ExpiryEstimate check(
      FoodMetadata food,
      ShelfLife life,
      ZoneKind zone,
      boolean opened,
      LocalDate date,
      ExpirySource source,
      LocalDate today) {
    Objects.requireNonNull(date, "date");
    Objects.requireNonNull(source, "source");
    if (date.isBefore(today)) {
      throw new ImpossibleExpiryException("The expiry date is in the past");
    }
    int days = life.daysIn(zone, opened);
    long away = ChronoUnit.DAYS.between(today, date);
    if (away > (long) days + marginDays) {
      throw new ImpossibleExpiryException(
          food.name()
              + " lasts about "
              + days
              + " days "
              + place(zone, opened)
              + ", so a date "
              + away
              + " days away is not possible; check the label or let HayPaComer estimate it");
    }
    return new ExpiryEstimate(date, source, days);
  }

  public LocalDate afterOpening(ShelfLife life, ZoneKind zone, LocalDate current, LocalDate today) {
    LocalDate opened = today.plusDays(life.daysIn(zone, true));
    return current == null || opened.isBefore(current) ? opened : current;
  }

  private static String place(ZoneKind zone, boolean opened) {
    if (zone == ZoneKind.FREEZER) {
      return "in the freezer";
    }
    if (opened) {
      return "once opened";
    }
    return zone == ZoneKind.DOOR ? "in the fridge door" : "in the fridge";
  }
}
