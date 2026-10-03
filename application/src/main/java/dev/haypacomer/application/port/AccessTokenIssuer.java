package dev.haypacomer.application.port;

import dev.haypacomer.application.auth.AccessToken;
import dev.haypacomer.domain.identity.UserId;
import java.time.Instant;

public interface AccessTokenIssuer {

  AccessToken issue(UserId user, Instant now);
}
