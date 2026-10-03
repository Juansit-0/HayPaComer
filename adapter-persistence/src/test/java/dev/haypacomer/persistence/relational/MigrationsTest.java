package dev.haypacomer.persistence.relational;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class MigrationsTest extends PostgresTestSupport {

  @Test
  void appliesAllMigrationsAndSeedsAllergens() {
    JdbcClient jdbc = JdbcClient.create(dataSource);

    assertEquals(2, appliedMigrations);
    assertEquals(14, jdbc.sql("SELECT count(*) FROM allergens").query(Integer.class).single());
  }
}
