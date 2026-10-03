package dev.haypacomer.web.security;

import dev.haypacomer.domain.identity.UserId;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public final class CurrentUser {

  private CurrentUser() {}

  public static UserId of(Jwt jwt) {
    return new UserId(UUID.fromString(jwt.getSubject()));
  }
}
