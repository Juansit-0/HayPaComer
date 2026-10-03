package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.fridge.Tray;
import dev.haypacomer.domain.fridge.TrayId;
import dev.haypacomer.domain.fridge.Zone;
import dev.haypacomer.domain.fridge.ZoneId;
import dev.haypacomer.domain.fridge.ZoneKind;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.quantity.Grams;
import java.sql.Types;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class PostgresFridgeRepository implements FridgeRepository {

  private final JdbcClient jdbc;
  private final TransactionTemplate transaction;

  public PostgresFridgeRepository(DataSource dataSource) {
    this.jdbc = JdbcClient.create(dataSource);
    this.transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
  }

  @Override
  public void save(HouseholdId household, Fridge fridge) {
    transaction.executeWithoutResult(status -> write(household.value(), fridge));
  }

  @Override
  public Optional<Fridge> findById(FridgeId id) {
    return jdbc.sql("SELECT id, name FROM fridges WHERE id = :id")
        .param("id", id.value())
        .query(
            (row, rowNumber) ->
                new Fridge(new FridgeId(row.getObject("id", UUID.class)), row.getString("name")))
        .optional()
        .map(this::loadTree);
  }

  @Override
  public List<Fridge> findByHousehold(HouseholdId household) {
    return jdbc
        .sql("SELECT id FROM fridges WHERE household_id = :household ORDER BY name, id")
        .param("household", household.value())
        .query(UUID.class)
        .list()
        .stream()
        .map(id -> findById(new FridgeId(id)).orElseThrow())
        .toList();
  }

  private void write(UUID householdId, Fridge fridge) {
    UUID fridgeId = fridge.id().value();
    jdbc.sql(
            """
            INSERT INTO fridges (id, household_id, name) VALUES (:id, :household, :name)
            ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name
            """)
        .param("id", fridgeId)
        .param("household", householdId)
        .param("name", fridge.name())
        .update();
    Set<UUID> zones = new HashSet<>();
    Set<UUID> trays = new HashSet<>();
    Set<UUID> items = new HashSet<>();
    List<Zone> fridgeZones = fridge.children();
    for (int zoneIndex = 0; zoneIndex < fridgeZones.size(); zoneIndex++) {
      Zone zone = fridgeZones.get(zoneIndex);
      zones.add(zone.id().value());
      upsertZone(fridgeId, zone, zoneIndex);
      for (Tray tray : zone.children()) {
        trays.add(tray.id().value());
        upsertTray(zone.id().value(), tray);
        List<FoodItem> trayItems = tray.children();
        for (int itemIndex = 0; itemIndex < trayItems.size(); itemIndex++) {
          FoodItem item = trayItems.get(itemIndex);
          items.add(item.id().value());
          upsertItem(householdId, tray.id().value(), item, itemIndex);
        }
      }
    }
    deleteMissing(
        "SELECT i.id FROM food_items i JOIN trays t ON t.id = i.tray_id"
            + " JOIN zones z ON z.id = t.zone_id WHERE z.fridge_id = :fridge",
        "DELETE FROM food_items WHERE id = :id",
        fridgeId,
        items);
    deleteMissing(
        "SELECT t.id FROM trays t JOIN zones z ON z.id = t.zone_id WHERE z.fridge_id = :fridge",
        "DELETE FROM trays WHERE id = :id",
        fridgeId,
        trays);
    deleteMissing(
        "SELECT id FROM zones WHERE fridge_id = :fridge",
        "DELETE FROM zones WHERE id = :id",
        fridgeId,
        zones);
  }

  private void upsertZone(UUID fridgeId, Zone zone, int position) {
    jdbc.sql(
            """
            INSERT INTO zones (id, fridge_id, name, kind, position)
            VALUES (:id, :fridge, :name, :kind, :position)
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name, kind = EXCLUDED.kind, position = EXCLUDED.position
            """)
        .param("id", zone.id().value())
        .param("fridge", fridgeId)
        .param("name", zone.name())
        .param("kind", zone.kind().name())
        .param("position", position)
        .update();
  }

  private void upsertTray(UUID zoneId, Tray tray) {
    jdbc.sql(
            """
            INSERT INTO trays (id, zone_id, name, position) VALUES (:id, :zone, :name, :position)
            ON CONFLICT (id) DO UPDATE SET
                zone_id = EXCLUDED.zone_id, name = EXCLUDED.name, position = EXCLUDED.position
            """)
        .param("id", tray.id().value())
        .param("zone", zoneId)
        .param("name", tray.name())
        .param("position", tray.position())
        .update();
  }

  private void upsertItem(UUID householdId, UUID trayId, FoodItem item, int position) {
    UUID foodId =
        jdbc.sql("SELECT id FROM food_catalog WHERE name_key = :key")
            .param("key", item.food().key())
            .query(UUID.class)
            .optional()
            .orElseThrow(
                () -> new IllegalStateException("Food not in catalog: " + item.food().name()));
    jdbc.sql(
            """
            INSERT INTO food_items (id, household_id, tray_id, food_id, quantity_g, tare_g,
                                    expires_on, position)
            VALUES (:id, :household, :tray, :food, :quantity, :tare, :expiresOn, :position)
            ON CONFLICT (id) DO UPDATE SET
                tray_id = EXCLUDED.tray_id,
                quantity_g = EXCLUDED.quantity_g,
                expires_on = EXCLUDED.expires_on,
                position = EXCLUDED.position,
                version = food_items.version + 1
            """)
        .param("id", item.id().value())
        .param("household", householdId)
        .param("tray", trayId)
        .param("food", foodId)
        .param("quantity", item.quantity().value())
        .param("tare", item.tare().value())
        .param("expiresOn", item.expiresOn().orElse(null), Types.DATE)
        .param("position", position)
        .update();
  }

  private void deleteMissing(String selectSql, String deleteSql, UUID fridgeId, Set<UUID> keep) {
    jdbc.sql(selectSql).param("fridge", fridgeId).query(UUID.class).list().stream()
        .filter(id -> !keep.contains(id))
        .forEach(id -> jdbc.sql(deleteSql).param("id", id).update());
  }

  private Fridge loadTree(Fridge fridge) {
    UUID fridgeId = fridge.id().value();
    Map<UUID, FoodMetadata> foods =
        FoodRows.load(
            jdbc,
            "WHERE id IN (SELECT i.food_id FROM food_items i JOIN trays t ON t.id = i.tray_id"
                + " JOIN zones z ON z.id = t.zone_id WHERE z.fridge_id = :fridge)",
            Map.of("fridge", fridgeId));
    jdbc.sql("SELECT id, name, kind FROM zones WHERE fridge_id = :fridge ORDER BY position, id")
        .param("fridge", fridgeId)
        .query(
            (row, rowNumber) ->
                new Zone(
                    new ZoneId(row.getObject("id", UUID.class)),
                    row.getString("name"),
                    ZoneKind.valueOf(row.getString("kind"))))
        .list()
        .forEach(
            zone -> {
              fridge.add(zone);
              loadTrays(zone, foods);
            });
    return fridge;
  }

  private void loadTrays(Zone zone, Map<UUID, FoodMetadata> foods) {
    jdbc.sql("SELECT id, name, position FROM trays WHERE zone_id = :zone ORDER BY position, id")
        .param("zone", zone.id().value())
        .query(
            (row, rowNumber) ->
                new Tray(
                    new TrayId(row.getObject("id", UUID.class)),
                    row.getString("name"),
                    row.getInt("position")))
        .list()
        .forEach(
            tray -> {
              zone.add(tray);
              loadItems(tray, foods);
            });
  }

  private void loadItems(Tray tray, Map<UUID, FoodMetadata> foods) {
    jdbc.sql(
            """
            SELECT id, food_id, quantity_g, tare_g, expires_on FROM food_items
            WHERE tray_id = :tray ORDER BY position, id
            """)
        .param("tray", tray.id().value())
        .query(
            (row, rowNumber) ->
                new FoodItem(
                    new FoodItemId(row.getObject("id", UUID.class)),
                    foods.get(row.getObject("food_id", UUID.class)),
                    Grams.of(row.getBigDecimal("quantity_g")),
                    Grams.of(row.getBigDecimal("tare_g")),
                    row.getObject("expires_on", LocalDate.class)))
        .list()
        .forEach(tray::add);
  }
}
