package dev.haypacomer.application.port;

import dev.haypacomer.domain.identity.PasswordHash;

public interface PasswordHasher {

  PasswordHash hash(String rawPassword);

  boolean matches(String rawPassword, PasswordHash hash);
}
