package dev.haypacomer.application.port;

import java.time.Duration;
import java.util.Optional;

public interface AiResponseCache {

  Optional<String> get(String key);

  void put(String key, String json, Duration timeToLive);
}
