package dev.haypacomer.domain.quantity;

import java.math.BigDecimal;
import java.util.Objects;

public record Quantity(BigDecimal amount, Unit unit) {

  public Quantity {
    Objects.requireNonNull(amount, "amount");
    Objects.requireNonNull(unit, "unit");
    if (amount.signum() < 0) {
      throw new IllegalArgumentException("Quantity cannot be negative: " + amount);
    }
  }

  public static Quantity of(String amount, Unit unit) {
    return new Quantity(new BigDecimal(amount), unit);
  }

  public Quantity convertTo(Unit target) {
    if (!unit.isCompatibleWith(target)) {
      throw new UnconvertibleQuantityException(
          "Cannot convert " + unit.symbol() + " to " + target.symbol());
    }
    return new Quantity(target.fromBase(unit.toBase(amount)), target);
  }

  public Grams toGrams(ConversionFactors factors) {
    return factors.toGrams(amount, unit);
  }

  @Override
  public String toString() {
    return amount.stripTrailingZeros().toPlainString() + " " + unit.symbol();
  }
}
