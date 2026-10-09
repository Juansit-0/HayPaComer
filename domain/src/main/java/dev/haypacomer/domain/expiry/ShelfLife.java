package dev.haypacomer.domain.expiry;

import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.ZoneKind;

public record ShelfLife(int fridgeDays, int doorDays, int freezerDays, int openedDays) {

  private static final int FREEZER_FALLBACK_DAYS = 90;

  public ShelfLife {
    if (fridgeDays < 0 || doorDays < 0 || freezerDays < 0 || openedDays < 0) {
      throw new IllegalArgumentException("Shelf life days cannot be negative");
    }
  }

  public static ShelfLife of(FoodMetadata food) {
    int days = food.shelfDays();
    return new ShelfLife(days, days, Math.max(days, FREEZER_FALLBACK_DAYS), days);
  }

  public int daysIn(ZoneKind zone, boolean opened) {
    int stored =
        switch (zone) {
          case SHELF, DRAWER -> fridgeDays;
          case DOOR -> doorDays;
          case FREEZER -> freezerDays;
        };
    return opened && zone != ZoneKind.FREEZER ? Math.min(stored, openedDays) : stored;
  }
}
