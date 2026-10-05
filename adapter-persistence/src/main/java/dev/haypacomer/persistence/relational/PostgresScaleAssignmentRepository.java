package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.port.ScaleAssignmentRepository;
import dev.haypacomer.application.scale.ScaleAssignment;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresScaleAssignmentRepository implements ScaleAssignmentRepository {

  private final JdbcClient jdbc;

  public PostgresScaleAssignmentRepository(DataSource dataSource) {
    this.jdbc = JdbcClient.create(dataSource);
  }

  @Override
  public void save(ScaleAssignment assignment) {
    jdbc.sql(
            """
            INSERT INTO scale_assignments (device_id, household_id, food_item_id, assigned_by,
                                           assigned_at)
            VALUES (:device, :household, :item, :by, :at)
            ON CONFLICT (device_id) DO UPDATE SET
                food_item_id = EXCLUDED.food_item_id,
                assigned_by = EXCLUDED.assigned_by,
                assigned_at = EXCLUDED.assigned_at
            """)
        .param("device", assignment.scale().value())
        .param("household", assignment.household().value())
        .param("item", assignment.item().value())
        .param("by", assignment.assignedBy().value())
        .param("at", Timestamps.toDatabase(assignment.assignedAt()))
        .update();
  }

  @Override
  public Optional<ScaleAssignment> find(DeviceId scale) {
    return jdbc.sql(
            "SELECT device_id, household_id, food_item_id, assigned_by, assigned_at"
                + " FROM scale_assignments WHERE device_id = :device")
        .param("device", scale.value())
        .query(
            (row, rowNumber) ->
                new ScaleAssignment(
                    new DeviceId(row.getObject("device_id", UUID.class)),
                    new HouseholdId(row.getObject("household_id", UUID.class)),
                    new FoodItemId(row.getObject("food_item_id", UUID.class)),
                    new UserId(row.getObject("assigned_by", UUID.class)),
                    Timestamps.read(row, "assigned_at")))
        .optional();
  }

  @Override
  public void remove(DeviceId scale) {
    jdbc.sql("DELETE FROM scale_assignments WHERE device_id = :device")
        .param("device", scale.value())
        .update();
  }
}
