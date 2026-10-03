package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.port.SensorEventLog;
import dev.haypacomer.application.sensor.validation.SensorEventTypes;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.sensor.DoorEvent;
import dev.haypacomer.domain.sensor.SensorEvent;
import dev.haypacomer.domain.sensor.SensorEventId;
import dev.haypacomer.domain.sensor.TemperatureReading;
import dev.haypacomer.domain.sensor.WeightReading;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresSensorEventLog implements SensorEventLog {

  private final JdbcClient jdbc;

  public PostgresSensorEventLog(DataSource dataSource) {
    this.jdbc = JdbcClient.create(dataSource);
  }

  @Override
  public boolean contains(SensorEventId id) {
    return jdbc.sql("SELECT EXISTS (SELECT 1 FROM sensor_events WHERE id = :id)")
        .param("id", id.value())
        .query(Boolean.class)
        .single();
  }

  @Override
  public Optional<Instant> lastAccepted(DeviceId device, String type) {
    return jdbc.sql(
            "SELECT max(occurred_at) FROM sensor_events WHERE device_id = :device AND type = :type")
        .param("device", device.value())
        .param("type", type)
        .query((row, rowNumber) -> row.getObject(1, OffsetDateTime.class))
        .optional()
        .map(OffsetDateTime::toInstant);
  }

  @Override
  public void accept(SensorEvent event, Instant receivedAt) {
    String door = event instanceof DoorEvent value ? value.state().name() : null;
    var celsius = event instanceof TemperatureReading value ? value.celsius() : null;
    var grams = event instanceof WeightReading value ? value.grams().value() : null;
    Boolean stable = event instanceof WeightReading value ? value.stable() : null;
    String mode = event instanceof WeightReading value ? value.mode().name() : null;
    jdbc.sql(
            """
            INSERT INTO sensor_events (id, device_id, fridge_id, type, door_state, celsius, grams,
                                       stable, scale_mode, occurred_at, received_at)
            VALUES (:id, :device, :fridge, :type, :door, :celsius, :grams, :stable, :mode,
                    :occurredAt, :receivedAt)
            ON CONFLICT (id) DO NOTHING
            """)
        .param("id", event.id().value())
        .param("device", event.device().value())
        .param("fridge", event.fridge().value())
        .param("type", SensorEventTypes.of(event))
        .param("door", door, Types.VARCHAR)
        .param("celsius", celsius, Types.NUMERIC)
        .param("grams", grams, Types.NUMERIC)
        .param("stable", stable, Types.BOOLEAN)
        .param("mode", mode, Types.VARCHAR)
        .param("occurredAt", Timestamps.toDatabase(event.occurredAt()))
        .param("receivedAt", Timestamps.toDatabase(receivedAt))
        .update();
  }
}
