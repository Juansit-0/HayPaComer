package dev.haypacomer.domain.quantity;

public final class InvalidQuantityException extends RuntimeException {

  public InvalidQuantityException(String message) {
    super(message);
  }
}
