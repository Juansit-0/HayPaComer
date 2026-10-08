package dev.haypacomer.application.port;

import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.session.CookingSession;
import dev.haypacomer.domain.session.CookingSessionId;
import java.util.Optional;

public interface CookingSessionRepository {

  void save(CookingSession session);

  Optional<CookingSession> find(HouseholdId household, CookingSessionId id);

  Optional<CookingSession> active(HouseholdId household);
}
