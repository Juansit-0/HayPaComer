package dev.haypacomer.persistence.relational;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class SensorPartitionsTest extends PostgresTestSupport {

  private int partitions() {
    return JdbcClient.create(dataSource)
        .sql("SELECT count(*) FROM pg_inherits" + " WHERE inhparent = 'sensor_events'::regclass")
        .query(Integer.class)
        .single();
  }

  @Test
  void sensorEventsArePartitionedByMonthWithADefaultAndAreKeptAhead() {
    int before = partitions();
    assertTrue(before >= 5, "partitions: " + before);

    PostgresSensorPartitions maintenance = new PostgresSensorPartitions(dataSource);
    assertEquals(0, maintenance.ensureAhead(LocalDate.now()));
    int added = maintenance.ensureAhead(LocalDate.now().minusMonths(2));

    assertEquals(before + added, partitions());
    assertEquals(0, maintenance.ensureAhead(LocalDate.now().minusMonths(2)));
    assertEquals(
        "p",
        JdbcClient.create(dataSource)
            .sql("SELECT relkind::text FROM pg_class WHERE relname = 'sensor_events'")
            .query(String.class)
            .single());
  }
}
