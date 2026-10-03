package dev.haypacomer.application.port;

import dev.haypacomer.domain.identity.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenStore {

  void save(RefreshToken token);

  Optional<RefreshToken> findByHash(String tokenHash);

  void revokeFamily(UUID family, Instant at);
}
