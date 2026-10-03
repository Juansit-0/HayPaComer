package dev.haypacomer.application.auth;

import java.time.Duration;

public record AuthSettings(
    Duration refreshTokenTimeToLive,
    int maxFailedLogins,
    Duration lockoutWindow,
    int minPasswordLength) {

  public static final AuthSettings DEFAULT =
      new AuthSettings(Duration.ofDays(30), 5, Duration.ofMinutes(15), 10);

  public AuthSettings {
    if (refreshTokenTimeToLive.isNegative() || refreshTokenTimeToLive.isZero()) {
      throw new IllegalArgumentException("Refresh token lifetime must be positive");
    }
    if (maxFailedLogins < 1 || minPasswordLength < 8) {
      throw new IllegalArgumentException("Login limits are too weak");
    }
  }
}
