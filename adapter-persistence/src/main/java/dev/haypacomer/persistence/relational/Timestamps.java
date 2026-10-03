package dev.haypacomer.persistence.relational;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

final class Timestamps {

  private Timestamps() {}

  static OffsetDateTime toDatabase(Instant instant) {
    return instant.atOffset(ZoneOffset.UTC);
  }

  static Instant read(ResultSet row, String column) throws SQLException {
    return row.getObject(column, OffsetDateTime.class).toInstant();
  }
}
