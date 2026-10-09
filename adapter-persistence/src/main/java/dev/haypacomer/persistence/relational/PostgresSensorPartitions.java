package dev.haypacomer.persistence.relational;

import java.time.LocalDate;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresSensorPartitions {

  public static final int MONTHS_AHEAD = 3;

  private final JdbcClient jdbc;

  public PostgresSensorPartitions(DataSource dataSource) {
    this.jdbc = JdbcClient.create(dataSource);
  }

  public int ensureAhead(LocalDate today) {
    return jdbc.sql("SELECT ensure_sensor_event_partitions(:first, :ahead)")
        .param("first", today.withDayOfMonth(1))
        .param("ahead", MONTHS_AHEAD)
        .query(Integer.class)
        .single();
  }
}
