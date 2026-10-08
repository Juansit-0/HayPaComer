package dev.haypacomer.application.port;

import dev.haypacomer.application.agent.PendingConfirmation;
import dev.haypacomer.domain.identity.UserId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConfirmationStore {

  void propose(PendingConfirmation confirmation);

  Optional<PendingConfirmation> find(UUID id);

  List<PendingConfirmation> pendingFor(UserId user);

  void remove(PendingConfirmation confirmation);
}
