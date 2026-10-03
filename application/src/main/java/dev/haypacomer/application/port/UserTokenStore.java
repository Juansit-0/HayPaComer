package dev.haypacomer.application.port;

import dev.haypacomer.domain.identity.UserToken;
import java.util.Optional;

public interface UserTokenStore {

  void save(UserToken token);

  Optional<UserToken> findByHash(String tokenHash);
}
