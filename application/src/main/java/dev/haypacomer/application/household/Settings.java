package dev.haypacomer.application.household;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Currency;

final class Settings {

  private Settings() {}

  static Currency currency(String code) {
    try {
      return Currency.getInstance(code);
    } catch (IllegalArgumentException | NullPointerException exception) {
      throw new IllegalArgumentException("Unknown currency: " + code);
    }
  }

  static ZoneId timezone(String id) {
    try {
      return ZoneId.of(id);
    } catch (DateTimeException | NullPointerException exception) {
      throw new IllegalArgumentException("Unknown timezone: " + id);
    }
  }
}
