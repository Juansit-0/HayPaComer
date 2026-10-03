package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.port.ColdChainRepository;
import dev.haypacomer.domain.coldchain.ColdChain;
import dev.haypacomer.domain.coldchain.ColdChainPhase;
import dev.haypacomer.domain.coldchain.ColdChainState;
import dev.haypacomer.domain.coldchain.ColdIncident;
import dev.haypacomer.domain.coldchain.Normal;
import dev.haypacomer.domain.coldchain.UnderReview;
import dev.haypacomer.domain.coldchain.Warming;
import dev.haypacomer.domain.fridge.FridgeId;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class PostgresColdChainRepository implements ColdChainRepository {

  private final JdbcClient jdbc;
  private final TransactionTemplate transaction;

  public PostgresColdChainRepository(DataSource dataSource) {
    this.jdbc = JdbcClient.create(dataSource);
    this.transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
  }

  @Override
  public Optional<ColdChain> find(FridgeId fridge) {
    return jdbc.sql(
            "SELECT fridge_id, phase, since, peak_c, recovered, last_c, last_reading_at"
                + " FROM cold_chains WHERE fridge_id = :fridge")
        .param("fridge", fridge.value())
        .query(this::map)
        .optional();
  }

  @Override
  public void save(ColdChain chain) {
    transaction.executeWithoutResult(status -> write(chain));
  }

  private void write(ColdChain chain) {
    ColdChainState state = chain.state();
    jdbc.sql(
            """
            INSERT INTO cold_chains (fridge_id, phase, since, peak_c, recovered, last_c,
                                     last_reading_at, updated_at)
            VALUES (:fridge, :phase, :since, :peak, :recovered, :last, :lastAt, now())
            ON CONFLICT (fridge_id) DO UPDATE SET
                phase = EXCLUDED.phase,
                since = EXCLUDED.since,
                peak_c = EXCLUDED.peak_c,
                recovered = EXCLUDED.recovered,
                last_c = EXCLUDED.last_c,
                last_reading_at = EXCLUDED.last_reading_at,
                updated_at = now()
            """)
        .param("fridge", chain.fridge().value())
        .param("phase", state.phase().name())
        .param(
            "since",
            state.since().map(Timestamps::toDatabase).orElse(null),
            Types.TIMESTAMP_WITH_TIMEZONE)
        .param("peak", state.peak().orElse(null), Types.NUMERIC)
        .param("recovered", state.recovered())
        .param("last", chain.lastCelsius().orElse(null), Types.NUMERIC)
        .param(
            "lastAt",
            chain.lastReadingAt().map(Timestamps::toDatabase).orElse(null),
            Types.TIMESTAMP_WITH_TIMEZONE)
        .update();
    for (ColdIncident incident : chain.closedIncidents()) {
      jdbc.sql(
              """
              INSERT INTO cold_incidents (fridge_id, started_at, reviewed_at, max_c, reviewed_by)
              VALUES (:fridge, :started, :reviewed, :peak, (SELECT id FROM users WHERE id = :by))
              ON CONFLICT (fridge_id, started_at) DO NOTHING
              """)
          .param("fridge", chain.fridge().value())
          .param("started", Timestamps.toDatabase(incident.startedAt()))
          .param("reviewed", Timestamps.toDatabase(incident.reviewedAt()))
          .param("peak", incident.peakCelsius())
          .param("by", incident.reviewedBy().value())
          .update();
    }
  }

  private ColdChain map(ResultSet row, int rowNumber) throws SQLException {
    Instant since = instant(row, "since");
    BigDecimal peak = row.getBigDecimal("peak_c");
    ColdChainState state =
        switch (ColdChainPhase.valueOf(row.getString("phase"))) {
          case NORMAL -> new Normal();
          case WARMING -> new Warming(since, peak);
          case UNDER_REVIEW -> new UnderReview(since, peak, row.getBoolean("recovered"));
        };
    return ColdChain.restore(
        new FridgeId(row.getObject("fridge_id", UUID.class)),
        state,
        row.getBigDecimal("last_c"),
        instant(row, "last_reading_at"));
  }

  private static Instant instant(ResultSet row, String column) throws SQLException {
    OffsetDateTime value = row.getObject(column, OffsetDateTime.class);
    return value == null ? null : value.toInstant();
  }
}
