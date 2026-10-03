package dev.haypacomer.web.security;

import dev.haypacomer.application.port.PasswordHasher;
import dev.haypacomer.domain.identity.PasswordHash;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCryptPasswordHasher implements PasswordHasher {

  private static final int STRENGTH = 12;

  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(STRENGTH);

  @Override
  public PasswordHash hash(String rawPassword) {
    return new PasswordHash(encoder.encode(rawPassword));
  }

  @Override
  public boolean matches(String rawPassword, PasswordHash hash) {
    return encoder.matches(rawPassword, hash.value());
  }
}
