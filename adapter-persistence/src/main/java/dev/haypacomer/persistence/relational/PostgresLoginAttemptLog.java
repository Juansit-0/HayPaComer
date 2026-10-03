package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.port.LoginAttemptLog;
import dev.haypacomer.domain.identity.EmailAddress;
import java.time.Instant;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresLoginAttemptLog implements LoginAttemptLog {

  private final JdbcClient jdbc;

  public PostgresLoginAttemptLog(DataSource dataSource) {
    this.jdbc = JdbcClient.create(dataSource);
  }

  @Override
  public void record(EmailAddress email, boolean success, Instant at) {
    jdbc.sql(
            """
            INSERT INTO login_attempts (email, user_id, success, at)
            VALUES (:email, (SELECT id FROM users WHERE email = :email), :success, :at)
            """)
        .param("email", email.value())
        .param("success", success)
        .param("at", Timestamps.toDatabase(at))
        .update();
  }

  @Override
  public int failuresSince(EmailAddress email, Instant since) {
    return jdbc.sql(
            """
            SELECT count(*) FROM login_attempts
            WHERE email = :email AND NOT success AND at >= :since
            """)
        .param("email", email.value())
        .param("since", Timestamps.toDatabase(since))
        .query(Integer.class)
        .single();
  }
}
