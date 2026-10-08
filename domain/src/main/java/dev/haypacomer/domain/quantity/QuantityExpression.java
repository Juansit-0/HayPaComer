package dev.haypacomer.domain.quantity;

public sealed interface QuantityExpression permits Amount, Sum {

  Grams interpret(ConversionFactors factors);
}
