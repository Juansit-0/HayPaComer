package dev.haypacomer.persistence.relational;

import static dev.haypacomer.persistence.relational.PersistenceFixtures.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.identity.EmailAddress;
import dev.haypacomer.domain.identity.User;
import dev.haypacomer.domain.identity.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class PostgresUserRepositoryTest extends PostgresTestSupport {

  private PostgresUserRepository users;

  @BeforeEach
  void createRepository() {
    users = new PostgresUserRepository(dataSource);
  }

  @Test
  void savesAndFindsByIdAndEmail() {
    User juan = user("juan@haypacomer.dev", "Juan");
    users.save(juan);

    assertEquals(juan, users.findById(juan.id()).orElseThrow());
    assertEquals(juan, users.findByEmail(new EmailAddress("JUAN@haypacomer.dev")).orElseThrow());
    assertTrue(users.findById(UserId.newId()).isEmpty());
  }

  @Test
  void updatesExistingUser() {
    User juan = user("juan@haypacomer.dev", "Juan");
    users.save(juan);

    User verified = juan.verifyEmail().rename("Juanca");
    users.save(verified);

    assertEquals(verified, users.findById(juan.id()).orElseThrow());
  }

  @Test
  void rejectsDuplicateEmail() {
    users.save(user("ana@haypacomer.dev", "Ana"));

    assertThrows(
        DataIntegrityViolationException.class,
        () -> users.save(user("ana@haypacomer.dev", "Other Ana")));
  }
}
