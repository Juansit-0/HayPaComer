package dev.haypacomer.ai.resilience;

import dev.haypacomer.application.ai.AdvisorSource;
import dev.haypacomer.application.ai.CircuitPhase;
import dev.haypacomer.application.ai.CircuitPolicy;
import dev.haypacomer.application.ai.CircuitState;
import dev.haypacomer.application.port.CircuitBreakerStore;
import dev.haypacomer.application.port.ServiceHealth;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class ProviderCircuit {

  private final AdvisorSource source;
  private final CircuitBreakerStore store;
  private final Supplier<CircuitPolicy> policies;
  private final ServiceHealth health;
  private final Clock clock;

  public ProviderCircuit(
      AdvisorSource source,
      CircuitBreakerStore store,
      CircuitPolicy policy,
      ServiceHealth health,
      Clock clock) {
    this(source, store, constant(policy), health, clock);
  }

  public ProviderCircuit(
      AdvisorSource source,
      CircuitBreakerStore store,
      Supplier<CircuitPolicy> policies,
      ServiceHealth health,
      Clock clock) {
    this.source = Objects.requireNonNull(source, "source");
    this.store = Objects.requireNonNull(store, "store");
    this.policies = Objects.requireNonNull(policies, "policies");
    this.health = Objects.requireNonNull(health, "health");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public AdvisorSource source() {
    return source;
  }

  public <T> T call(
      Supplier<T> provider, Supplier<T> fallback, Predicate<RuntimeException> outage) {
    Instant now = clock.instant();
    CircuitPolicy policy = policies.get();
    CircuitState state = store.load(source);
    if (!state.allows(now, policy)) {
      health.degraded(component(), "Resting after repeated failures; offline rules answer", now);
      return fallback.get();
    }
    CircuitState trying = state.attempt(now, policy);
    if (!trying.equals(state)) {
      store.save(source, trying);
    }
    try {
      T answer = provider.get();
      if (!trying.equals(CircuitState.CLOSED)) {
        store.save(source, trying.success());
      }
      health.recovered(component());
      return answer;
    } catch (RuntimeException failure) {
      if (!outage.test(failure)) {
        throw failure;
      }
      CircuitState after = trying.failure(clock.instant(), policy);
      store.save(source, after);
      if (after.phase() == CircuitPhase.OPEN) {
        health.degraded(
            component(), "Resting after repeated failures; offline rules answer", clock.instant());
      }
      return fallback.get();
    }
  }

  private static Supplier<CircuitPolicy> constant(CircuitPolicy policy) {
    Objects.requireNonNull(policy, "policy");
    return () -> policy;
  }

  private String component() {
    return "ai provider " + source.name().toLowerCase(Locale.ROOT).replace('_', '-');
  }
}
