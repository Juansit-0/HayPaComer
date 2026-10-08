package dev.haypacomer.domain.quantity;

import java.util.Objects;

public record Amount(Quantity quantity) implements QuantityExpression {

  public Amount {
    Objects.requireNonNull(quantity, "quantity");
  }

  @Override
  public Grams interpret(ConversionFactors factors) {
    return quantity.toGrams(factors);
  }

  @Override
  public String toString() {
    return quantity.toString();
  }
}
