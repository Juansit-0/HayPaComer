package dev.haypacomer.persistence.relational;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
abstract class PostgresTestSupport {

  @Container
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

  static DataSource dataSource;
  static int appliedMigrations;

  @BeforeAll
  static void migrate() {
    PGSimpleDataSource source = new PGSimpleDataSource();
    source.setUrl(POSTGRES.getJdbcUrl());
    source.setUser(POSTGRES.getUsername());
    source.setPassword(POSTGRES.getPassword());
    dataSource = source;
    appliedMigrations = Flyway.configure().dataSource(source).load().migrate().migrationsExecuted;
  }

  @BeforeEach
  void cleanTables() {
    JdbcClient.create(dataSource).sql("TRUNCATE users, households, food_catalog CASCADE").update();
  }
}
