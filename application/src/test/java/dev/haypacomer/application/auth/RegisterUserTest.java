package dev.haypacomer.application.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.haypacomer.domain.identity.User;
import org.junit.jupiter.api.Test;

class RegisterUserTest {

  private final AuthFakes fakes = new AuthFakes();
  private final RegisterUser registerUser = fakes.registerUser();

  @Test
  void registersWithHashedPassword() {
    User user =
        registerUser.register(new RegisterCommand("Juan@HayPaComer.dev", "fresh-milk-842", "Juan"));

    assertEquals("juan@haypacomer.dev", user.email().value());
    assertEquals("hashed:fresh-milk-842", user.passwordHash().value());
    assertFalse(user.emailVerified());
    assertEquals(user, fakes.users.findById(user.id()).orElseThrow());
    assertEquals(fakes.clock.instant(), user.createdAt());
  }

  @Test
  void rejectsDuplicateEmailIgnoringCase() {
    registerUser.register(new RegisterCommand("ana@haypacomer.dev", "fresh-milk-842", "Ana"));

    assertThrows(
        EmailAlreadyRegisteredException.class,
        () ->
            registerUser.register(
                new RegisterCommand("ANA@haypacomer.dev", "another-pass-1", "Ana")));
  }

  @Test
  void enforcesPasswordLength() {
    assertThrows(
        InvalidPasswordException.class,
        () -> registerUser.register(new RegisterCommand("a@b.co", "short", "A")));
    assertThrows(
        InvalidPasswordException.class,
        () -> registerUser.register(new RegisterCommand("a@b.co", " ", "A")));
    assertThrows(
        InvalidPasswordException.class,
        () -> registerUser.register(new RegisterCommand("a@b.co", null, "A")));
    assertThrows(
        InvalidPasswordException.class,
        () -> registerUser.register(new RegisterCommand("a@b.co", "x".repeat(129), "A")));
  }

  @Test
  void rejectsInvalidEmail() {
    assertThrows(
        IllegalArgumentException.class,
        () -> registerUser.register(new RegisterCommand("nope", "fresh-milk-842", "A")));
  }

  @Test
  void commandNeverPrintsPassword() {
    assertFalse(new RegisterCommand("a@b.co", "secret-pass-1", "A").toString().contains("secret"));
    assertFalse(new LoginCommand("a@b.co", "secret-pass-1").toString().contains("secret"));
  }
}
