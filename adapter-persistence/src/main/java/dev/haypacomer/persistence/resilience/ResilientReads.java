package dev.haypacomer.persistence.resilience;

import dev.haypacomer.application.port.ServiceHealth;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class ResilientReads {

  static final int CAPACITY = 1000;

  private final String component;
  private final RetryPolicy retry;
  private final ServiceHealth health;
  private final Clock clock;
  private final Map<String, Object> lastKnown =
      new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Object> eldest) {
          return size() > CAPACITY;
        }
      };

  public ResilientReads(String component, RetryPolicy retry, ServiceHealth health, Clock clock) {
    this.component = component;
    this.retry = retry;
    this.health = health;
    this.clock = clock;
  }

  @SuppressWarnings("unchecked")
  public <T> T read(String key, Supplier<T> call) {
    try {
      T value = retry.run(call);
      synchronized (lastKnown) {
        lastKnown.put(key, value);
      }
      health.recovered(component);
      return value;
    } catch (IllegalArgumentException | IllegalStateException rule) {
      throw rule;
    } catch (RuntimeException failure) {
      Object cached;
      synchronized (lastKnown) {
        cached = lastKnown.get(key);
      }
      health.degraded(
          component,
          cached == null ? "Unavailable and nothing saved yet" : "Serving the last saved copy",
          clock.instant());
      if (cached == null) {
        throw failure;
      }
      return (T) cached;
    }
  }

  public <T> T write(Supplier<T> call) {
    T value = retry.run(call);
    synchronized (lastKnown) {
      lastKnown.clear();
    }
    return value;
  }
}
