package dev.haypacomer.application.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.haypacomer.domain.identity.User;
import dev.haypacomer.domain.identity.UserId;
import org.junit.jupiter.api.Test;

class GetUserProfileTest {

  private final AuthFakes fakes = new AuthFakes();
  private final GetUserProfile getUserProfile = new GetUserProfile(fakes.users);

  @Test
  void returnsActiveUser() {
    User juan =
        fakes
            .registerUser()
            .register(new RegisterCommand("juan@haypacomer.dev", "fresh-milk-842", "Juan"));

    assertEquals(juan, getUserProfile.get(juan.id()));
  }

  @Test
  void hidesMissingAndDisabledUsers() {
    User juan =
        fakes
            .registerUser()
            .register(new RegisterCommand("juan@haypacomer.dev", "fresh-milk-842", "Juan"));
    fakes.users.save(juan.disable());

    assertThrows(UserNotFoundException.class, () -> getUserProfile.get(juan.id()));
    assertThrows(UserNotFoundException.class, () -> getUserProfile.get(UserId.newId()));
  }
}
