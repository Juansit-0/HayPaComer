package dev.haypacomer.application.settings;

import java.math.BigDecimal;
import java.util.Objects;

public record SettingDefinition(
    String key,
    SettingKind kind,
    SettingScope scope,
    String value,
    BigDecimal min,
    BigDecimal max,
    String description) {

  public SettingDefinition {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(scope, "scope");
    Objects.requireNonNull(value, "value");
    Objects.requireNonNull(min, "min");
    Objects.requireNonNull(max, "max");
    Objects.requireNonNull(description, "description");
  }

  public BigDecimal parse(String candidate) {
    BigDecimal number;
    try {
      number = new BigDecimal(candidate.strip());
    } catch (NumberFormatException notANumber) {
      throw new IllegalArgumentException(key + " must be a number");
    }
    if (kind == SettingKind.INTEGER && number.stripTrailingZeros().scale() > 0) {
      throw new IllegalArgumentException(key + " must be a whole number");
    }
    if (number.compareTo(min) < 0 || number.compareTo(max) > 0) {
      throw new IllegalArgumentException(
          key
              + " must be between "
              + min.stripTrailingZeros().toPlainString()
              + " and "
              + max.stripTrailingZeros().toPlainString());
    }
    return number;
  }
}
