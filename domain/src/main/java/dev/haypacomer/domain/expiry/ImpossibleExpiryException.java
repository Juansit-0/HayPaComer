package dev.haypacomer.domain.expiry;

public final class ImpossibleExpiryException extends RuntimeException {

  public ImpossibleExpiryException(String message) {
    super(message);
  }
}
