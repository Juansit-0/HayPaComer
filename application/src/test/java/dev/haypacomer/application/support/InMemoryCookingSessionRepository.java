package dev.haypacomer.application.support;

import dev.haypacomer.application.port.CookingSessionRepository;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.session.CookingSession;
import dev.haypacomer.domain.session.CookingSessionId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryCookingSessionRepository implements CookingSessionRepository {

  private final Map<CookingSessionId, CookingSession> sessions = new LinkedHashMap<>();

  @Override
  public void save(CookingSession session) {
    sessions.put(session.id(), session);
  }

  @Override
  public Optional<CookingSession> find(HouseholdId household, CookingSessionId id) {
    return Optional.ofNullable(sessions.get(id))
        .filter(session -> session.household().equals(household));
  }

  @Override
  public Optional<CookingSession> active(HouseholdId household) {
    return sessions.values().stream()
        .filter(session -> session.household().equals(household))
        .filter(session -> session.phase().active())
        .findFirst();
  }
}
