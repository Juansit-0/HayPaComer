package dev.haypacomer.domain.quantity;

import java.math.BigDecimal;
import java.math.MathContext;

public enum Unit {
  MILLIGRAM(UnitKind.MASS, "mg", "0.001"),
  GRAM(UnitKind.MASS, "g", "1"),
  KILOGRAM(UnitKind.MASS, "kg", "1000"),
  OUNCE(UnitKind.MASS, "oz", "28.349523125"),
  POUND(UnitKind.MASS, "lb", "453.59237"),
  MILLILITER(UnitKind.VOLUME, "ml", "1"),
  LITER(UnitKind.VOLUME, "l", "1000"),
  TEASPOON(UnitKind.VOLUME, "tsp", "5"),
  TABLESPOON(UnitKind.VOLUME, "tbsp", "15"),
  CUP(UnitKind.VOLUME, "cup", "240"),
  PIECE(UnitKind.COUNT, "pc", "1");

  private final UnitKind kind;
  private final String symbol;
  private final BigDecimal baseFactor;

  Unit(UnitKind kind, String symbol, String baseFactor) {
    this.kind = kind;
    this.symbol = symbol;
    this.baseFactor = new BigDecimal(baseFactor);
  }

  public UnitKind kind() {
    return kind;
  }

  public String symbol() {
    return symbol;
  }

  public BigDecimal toBase(BigDecimal amount) {
    return amount.multiply(baseFactor);
  }

  public BigDecimal fromBase(BigDecimal baseAmount) {
    return baseAmount.divide(baseFactor, MathContext.DECIMAL64);
  }

  public boolean isCompatibleWith(Unit other) {
    return kind == other.kind;
  }
}
