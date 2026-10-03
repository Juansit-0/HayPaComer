package dev.haypacomer.application.port;

import dev.haypacomer.domain.identity.EmailAddress;
import dev.haypacomer.domain.identity.User;
import dev.haypacomer.domain.identity.UserId;
import java.util.Optional;

public interface UserRepository {

  void save(User user);

  Optional<User> findById(UserId id);

  Optional<User> findByEmail(EmailAddress email);
}
