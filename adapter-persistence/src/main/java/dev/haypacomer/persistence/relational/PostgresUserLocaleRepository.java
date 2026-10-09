package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.port.UserLocaleRepository;
import dev.haypacomer.domain.identity.UserId;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresUserLocaleRepository implements UserLocaleRepository {

  private final JdbcClient jdbc;

  public PostgresUserLocaleRepository(DataSource dataSource) {
    this.jdbc = JdbcClient.create(dataSource);
  }

  @Override
  public Optional<String> locale(UserId user) {
    return jdbc.sql("SELECT locale FROM users WHERE id = :user AND locale IS NOT NULL")
        .param("user", user.value())
        .query(String.class)
        .optional();
  }

  @Override
  public void save(UserId user, String locale) {
    jdbc.sql("UPDATE users SET locale = :locale WHERE id = :user")
        .param("user", user.value())
        .param("locale", locale)
        .update();
  }
}
