package dev.haypacomer.application.household;

import dev.haypacomer.application.port.InvitationRepository;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Invitation;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class InMemoryInvitations implements InvitationRepository {

  final Map<UUID, Invitation> byId = new LinkedHashMap<>();

  @Override
  public void save(Invitation invitation) {
    byId.put(invitation.id(), invitation);
  }

  @Override
  public Optional<Invitation> findByHash(String tokenHash) {
    return byId.values().stream().filter(item -> item.tokenHash().equals(tokenHash)).findFirst();
  }

  @Override
  public List<Invitation> findPending(HouseholdId household, Instant now) {
    return byId.values().stream()
        .filter(item -> item.household().equals(household) && item.isPending(now))
        .toList();
  }

  @Override
  public void delete(HouseholdId household, UUID invitationId) {
    byId.entrySet()
        .removeIf(
            entry ->
                entry.getKey().equals(invitationId)
                    && entry.getValue().household().equals(household));
  }
}
