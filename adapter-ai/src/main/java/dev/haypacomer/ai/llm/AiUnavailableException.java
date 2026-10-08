package dev.haypacomer.ai.llm;

public final class AiUnavailableException extends RuntimeException {

  public AiUnavailableException(String message) {
    super(message);
  }

  public AiUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
