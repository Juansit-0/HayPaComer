package dev.haypacomer.ai.llm;

public final class InvalidAiResponseException extends RuntimeException {

  public InvalidAiResponseException(String message) {
    super(message);
  }
}
