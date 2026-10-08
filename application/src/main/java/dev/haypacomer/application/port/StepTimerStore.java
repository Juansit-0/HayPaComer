package dev.haypacomer.application.port;

import dev.haypacomer.domain.session.CookingSessionId;
import dev.haypacomer.domain.session.StepTimer;
import java.util.List;
import java.util.Optional;

public interface StepTimerStore {

  void save(StepTimer timer);

  Optional<StepTimer> find(CookingSessionId session);

  void remove(CookingSessionId session);

  List<StepTimer> all();
}
