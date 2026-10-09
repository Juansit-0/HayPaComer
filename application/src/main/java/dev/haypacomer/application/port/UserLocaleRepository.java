package dev.haypacomer.application.port;

import dev.haypacomer.domain.identity.UserId;
import java.util.Optional;

public interface UserLocaleRepository {

  Optional<String> locale(UserId user);

  void save(UserId user, String locale);
}
