package dev.haypacomer.application.agent;

public final class ChatModelUnavailableException extends RuntimeException {

  public ChatModelUnavailableException(String message) {
    super(message);
  }
}
