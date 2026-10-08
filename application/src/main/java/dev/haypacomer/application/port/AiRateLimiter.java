package dev.haypacomer.application.port;

import java.time.Duration;

public interface AiRateLimiter {

  boolean tryAcquire(String subject, int limit, Duration window);
}
