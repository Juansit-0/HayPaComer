package dev.haypacomer.application.port;

import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Invitation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository {

  void save(Invitation invitation);

  Optional<Invitation> findByHash(String tokenHash);

  List<Invitation> findPending(HouseholdId household, Instant now);

  void delete(HouseholdId household, UUID invitationId);
}
