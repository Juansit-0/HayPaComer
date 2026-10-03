package dev.haypacomer.domain.identity;

import java.time.Duration;

public enum UserTokenType {
  VERIFY_EMAIL(Duration.ofHours(24)),
  RESET_PASSWORD(Duration.ofHours(1));

  private final Duration timeToLive;

  UserTokenType(Duration timeToLive) {
    this.timeToLive = timeToLive;
  }

  public Duration timeToLive() {
    return timeToLive;
  }
}
