package dev.haypacomer.domain.quantity;

import java.util.Objects;

public record Sum(QuantityExpression left, QuantityExpression right) implements QuantityExpression {

  public Sum {
    Objects.requireNonNull(left, "left");
    Objects.requireNonNull(right, "right");
  }

  @Override
  public Grams interpret(ConversionFactors factors) {
    return left.interpret(factors).plus(right.interpret(factors));
  }

  @Override
  public String toString() {
    return left + " + " + right;
  }
}
