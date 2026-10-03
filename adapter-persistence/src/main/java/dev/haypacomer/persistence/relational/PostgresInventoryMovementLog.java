package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.port.InventoryMovementLog;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.InventoryMovement;
import dev.haypacomer.domain.inventory.MovementSource;
import dev.haypacomer.domain.inventory.MovementType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresInventoryMovementLog implements InventoryMovementLog {

  private static final String SELECT =
      "SELECT command_id, household_id, food_item_id, user_id, type, delta_g, source, at"
          + " FROM inventory_movements";

  private final JdbcClient jdbc;

  public PostgresInventoryMovementLog(DataSource dataSource) {
    this.jdbc = JdbcClient.create(dataSource);
  }

  @Override
  public void record(InventoryMovement movement) {
    jdbc.sql(
            """
            INSERT INTO inventory_movements (food_item_id, household_id, user_id, type, delta_g,
                                             source, command_id, at)
            VALUES (:item, :household, :actor, :type, :delta, :source, :command, :at)
            ON CONFLICT (command_id) DO NOTHING
            """)
        .param("item", movement.item().value())
        .param("household", movement.household().value())
        .param("actor", movement.actor().value())
        .param("type", movement.type().name())
        .param("delta", movement.deltaGrams())
        .param("source", movement.source().name())
        .param("command", movement.commandId())
        .param("at", Timestamps.toDatabase(movement.at()))
        .update();
  }

  @Override
  public List<InventoryMovement> history(FoodItemId item) {
    return jdbc.sql(SELECT + " WHERE food_item_id = :item ORDER BY at, id")
        .param("item", item.value())
        .query(this::map)
        .list();
  }

  @Override
  public Optional<InventoryMovement> findByCommand(UUID commandId) {
    return jdbc.sql(SELECT + " WHERE command_id = :command")
        .param("command", commandId)
        .query(this::map)
        .optional();
  }

  private InventoryMovement map(ResultSet row, int rowNumber) throws SQLException {
    return new InventoryMovement(
        row.getObject("command_id", UUID.class),
        new HouseholdId(row.getObject("household_id", UUID.class)),
        new FoodItemId(row.getObject("food_item_id", UUID.class)),
        new UserId(row.getObject("user_id", UUID.class)),
        MovementType.valueOf(row.getString("type")),
        row.getBigDecimal("delta_g"),
        MovementSource.valueOf(row.getString("source")),
        Timestamps.read(row, "at"));
  }
}
