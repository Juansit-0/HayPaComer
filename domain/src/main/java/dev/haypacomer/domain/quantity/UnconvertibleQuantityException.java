package dev.haypacomer.domain.quantity;

public final class UnconvertibleQuantityException extends RuntimeException {

  public UnconvertibleQuantityException(String message) {
    super(message);
  }
}
