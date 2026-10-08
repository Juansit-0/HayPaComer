package dev.haypacomer.application.ai;

public final class AiRateLimitExceededException extends RuntimeException {

  public AiRateLimitExceededException() {
    super("Too many suggestion requests; try again in a minute");
  }
}
