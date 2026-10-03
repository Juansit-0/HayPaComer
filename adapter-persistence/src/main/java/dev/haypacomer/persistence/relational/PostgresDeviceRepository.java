package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.port.DeviceRepository;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.device.DeviceKind;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.HouseholdId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresDeviceRepository implements DeviceRepository {

  private static final String SELECT =
      "SELECT id, household_id, fridge_id, name, kind, api_key_hash, created_at, last_seen_at,"
          + " revoked_at FROM devices";

  private final JdbcClient jdbc;

  public PostgresDeviceRepository(DataSource dataSource) {
    this.jdbc = JdbcClient.create(dataSource);
  }

  @Override
  public void save(Device device) {
    jdbc.sql(
            """
            INSERT INTO devices (id, household_id, fridge_id, name, kind, api_key_hash, created_at,
                                 last_seen_at, revoked_at)
            VALUES (:id, :household, :fridge, :name, :kind, :hash, :createdAt, :lastSeenAt,
                    :revokedAt)
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                last_seen_at = EXCLUDED.last_seen_at,
                revoked_at = EXCLUDED.revoked_at
            """)
        .param("id", device.id().value())
        .param("household", device.household().value())
        .param("fridge", device.fridge().value())
        .param("name", device.name())
        .param("kind", device.kind().name())
        .param("hash", HexFormat.of().parseHex(device.apiKeyHash()))
        .param("createdAt", Timestamps.toDatabase(device.createdAt()))
        .param("lastSeenAt", nullable(device.lastSeenAt()), Types.TIMESTAMP_WITH_TIMEZONE)
        .param("revokedAt", nullable(device.revokedAt()), Types.TIMESTAMP_WITH_TIMEZONE)
        .update();
  }

  @Override
  public Optional<Device> findById(DeviceId id) {
    return jdbc.sql(SELECT + " WHERE id = :id").param("id", id.value()).query(this::map).optional();
  }

  @Override
  public Optional<Device> findByKeyHash(String apiKeyHash) {
    return jdbc.sql(SELECT + " WHERE api_key_hash = :hash")
        .param("hash", HexFormat.of().parseHex(apiKeyHash))
        .query(this::map)
        .optional();
  }

  @Override
  public List<Device> findByHousehold(HouseholdId household) {
    return jdbc.sql(SELECT + " WHERE household_id = :household ORDER BY created_at, id")
        .param("household", household.value())
        .query(this::map)
        .list();
  }

  private static OffsetDateTime nullable(Instant instant) {
    return instant == null ? null : Timestamps.toDatabase(instant);
  }

  private static Instant read(ResultSet row, String column) throws SQLException {
    OffsetDateTime value = row.getObject(column, OffsetDateTime.class);
    return value == null ? null : value.toInstant();
  }

  private Device map(ResultSet row, int rowNumber) throws SQLException {
    return new Device(
        new DeviceId(row.getObject("id", UUID.class)),
        new HouseholdId(row.getObject("household_id", UUID.class)),
        new FridgeId(row.getObject("fridge_id", UUID.class)),
        row.getString("name"),
        DeviceKind.valueOf(row.getString("kind")),
        HexFormat.of().formatHex(row.getBytes("api_key_hash")),
        Timestamps.read(row, "created_at"),
        read(row, "last_seen_at"),
        read(row, "revoked_at"));
  }
}
