package dev.haypacomer.application.i18n;

public final class UnknownLocaleException extends RuntimeException {

  public UnknownLocaleException(String locale) {
    super("Unknown locale " + locale);
  }
}
