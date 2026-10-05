package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.port.ScaleCalibrationRepository;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.scale.ScaleCalibration;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresScaleCalibrationRepository implements ScaleCalibrationRepository {

  private final JdbcClient jdbc;

  public PostgresScaleCalibrationRepository(DataSource dataSource) {
    this.jdbc = JdbcClient.create(dataSource);
  }

  @Override
  public Optional<ScaleCalibration> find(DeviceId device) {
    return jdbc.sql(
            "SELECT offset_counts, counts_per_gram, tared_at, calibrated_at"
                + " FROM scale_calibrations WHERE device_id = :device")
        .param("device", device.value())
        .query(
            (row, rowNumber) -> {
              OffsetDateTime calibratedAt = row.getObject("calibrated_at", OffsetDateTime.class);
              return new ScaleCalibration(
                  row.getLong("offset_counts"),
                  row.getBigDecimal("counts_per_gram"),
                  Timestamps.read(row, "tared_at"),
                  calibratedAt == null ? null : calibratedAt.toInstant());
            })
        .optional();
  }

  @Override
  public void save(DeviceId device, ScaleCalibration calibration) {
    jdbc.sql(
            """
            INSERT INTO scale_calibrations (device_id, offset_counts, counts_per_gram, tared_at,
                                            calibrated_at)
            VALUES (:device, :offset, :factor, :taredAt, :calibratedAt)
            ON CONFLICT (device_id) DO UPDATE SET
                offset_counts = EXCLUDED.offset_counts,
                counts_per_gram = EXCLUDED.counts_per_gram,
                tared_at = EXCLUDED.tared_at,
                calibrated_at = EXCLUDED.calibrated_at
            """)
        .param("device", device.value())
        .param("offset", calibration.offsetCounts())
        .param("factor", calibration.countsPerGram(), Types.NUMERIC)
        .param("taredAt", Timestamps.toDatabase(calibration.taredAt()))
        .param(
            "calibratedAt",
            calibration.calibratedAt() == null
                ? null
                : Timestamps.toDatabase(calibration.calibratedAt()),
            Types.TIMESTAMP_WITH_TIMEZONE)
        .update();
  }
}
