package dev.haypacomer.application.auth;

import dev.haypacomer.application.port.UserRepository;
import dev.haypacomer.domain.identity.User;
import dev.haypacomer.domain.identity.UserId;
import java.util.Objects;

public final class GetUserProfile {

  private final UserRepository users;

  public GetUserProfile(UserRepository users) {
    this.users = Objects.requireNonNull(users, "users");
  }

  public User get(UserId id) {
    return users.findById(id).filter(User::canSignIn).orElseThrow(UserNotFoundException::new);
  }
}
