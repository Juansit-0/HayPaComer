package dev.haypacomer.sensors.cooking;

import dev.haypacomer.application.port.StepTimerStore;
import dev.haypacomer.domain.session.CookingSessionId;
import dev.haypacomer.domain.session.StepTimer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryStepTimerStore implements StepTimerStore {

  private final Map<CookingSessionId, StepTimer> timers = new ConcurrentHashMap<>();

  @Override
  public void save(StepTimer timer) {
    timers.put(timer.session(), timer);
  }

  @Override
  public Optional<StepTimer> find(CookingSessionId session) {
    return Optional.ofNullable(timers.get(session));
  }

  @Override
  public void remove(CookingSessionId session) {
    timers.remove(session);
  }

  @Override
  public List<StepTimer> all() {
    return List.copyOf(timers.values());
  }
}
