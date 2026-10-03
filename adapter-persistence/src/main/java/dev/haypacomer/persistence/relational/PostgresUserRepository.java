package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.port.UserRepository;
import dev.haypacomer.domain.identity.EmailAddress;
import dev.haypacomer.domain.identity.PasswordHash;
import dev.haypacomer.domain.identity.User;
import dev.haypacomer.domain.identity.UserId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresUserRepository implements UserRepository {

  private static final String SELECT =
      "SELECT id, email, password_hash, display_name, email_verified, enabled, created_at"
          + " FROM users";

  private final JdbcClient jdbc;

  public PostgresUserRepository(DataSource dataSource) {
    this.jdbc = JdbcClient.create(dataSource);
  }

  @Override
  public void save(User user) {
    jdbc.sql(
            """
            INSERT INTO users (id, email, password_hash, display_name, email_verified, enabled,
                               created_at)
            VALUES (:id, :email, :passwordHash, :displayName, :emailVerified, :enabled, :createdAt)
            ON CONFLICT (id) DO UPDATE SET
                email = EXCLUDED.email,
                password_hash = EXCLUDED.password_hash,
                display_name = EXCLUDED.display_name,
                email_verified = EXCLUDED.email_verified,
                enabled = EXCLUDED.enabled,
                updated_at = now()
            """)
        .param("id", user.id().value())
        .param("email", user.email().value())
        .param("passwordHash", user.passwordHash().value())
        .param("displayName", user.displayName())
        .param("emailVerified", user.emailVerified())
        .param("enabled", user.enabled())
        .param("createdAt", Timestamps.toDatabase(user.createdAt()))
        .update();
  }

  @Override
  public Optional<User> findById(UserId id) {
    return jdbc.sql(SELECT + " WHERE id = :id").param("id", id.value()).query(this::map).optional();
  }

  @Override
  public Optional<User> findByEmail(EmailAddress email) {
    return jdbc.sql(SELECT + " WHERE email = :email")
        .param("email", email.value())
        .query(this::map)
        .optional();
  }

  private User map(ResultSet row, int rowNumber) throws SQLException {
    return new User(
        new UserId(row.getObject("id", UUID.class)),
        new EmailAddress(row.getString("email")),
        new PasswordHash(row.getString("password_hash")),
        row.getString("display_name"),
        row.getBoolean("email_verified"),
        row.getBoolean("enabled"),
        Timestamps.read(row, "created_at"));
  }
}
