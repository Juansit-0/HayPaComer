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
import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class PostgresFridgeRepository implements FridgeRepository {

  private final JdbcClient jdbc;
  private final JdbcTemplate batch;
  private final TransactionTemplate transaction;

  public PostgresFridgeRepository(DataSource dataSource) {
    this.jdbc = JdbcClient.create(dataSource);
    this.batch = new JdbcTemplate(dataSource);
    this.transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
  }

  @Override
  public void save(HouseholdId household, Fridge fridge) {
    transaction.executeWithoutResult(status -> write(household.value(), fridge));
  }

  @Override
  public Optional<Fridge> findById(FridgeId id) {
    return load("WHERE f.id = :key", id.value()).stream().findFirst();
  }

  @Override
  public List<Fridge> findByHousehold(HouseholdId household) {
    return load("WHERE f.household_id = :key", household.value());
  }

  private List<Fridge> load(String where, UUID key) {
    Map<UUID, Fridge> fridges = new LinkedHashMap<>();
    Map<UUID, Zone> zones = new HashMap<>();
    Map<UUID, Tray> trays = new HashMap<>();
    List<Object[]> items = new ArrayList<>();
    jdbc.sql(
            """
            SELECT f.id AS fridge_id, f.name AS fridge_name,
                   z.id AS zone_id, z.name AS zone_name, z.kind AS zone_kind,
                   t.id AS tray_id, t.name AS tray_name, t.position AS tray_position,
                   i.id AS item_id, i.food_id, i.quantity_g, i.tare_g, i.expires_on
            FROM fridges f
            LEFT JOIN zones z ON z.fridge_id = f.id
            LEFT JOIN trays t ON t.zone_id = z.id
            LEFT JOIN food_items i ON i.tray_id = t.id
            """
                + where
                + """

            ORDER BY f.name, f.id, z.position, z.id, t.position, t.id, i.position, i.id
            """)
        .param("key", key)
        .query(
            row -> {
              UUID fridgeId = row.getObject("fridge_id", UUID.class);
              Fridge fridge = fridges.get(fridgeId);
              if (fridge == null) {
                fridge = new Fridge(new FridgeId(fridgeId), row.getString("fridge_name"));
                fridges.put(fridgeId, fridge);
              }
              UUID zoneId = row.getObject("zone_id", UUID.class);
              if (zoneId == null) {
                return;
              }
              Zone zone = zones.get(zoneId);
              if (zone == null) {
                zone =
                    new Zone(
                        new ZoneId(zoneId),
                        row.getString("zone_name"),
                        ZoneKind.valueOf(row.getString("zone_kind")));
                zones.put(zoneId, zone);
                fridge.add(zone);
              }
              UUID trayId = row.getObject("tray_id", UUID.class);
              if (trayId == null) {
                return;
              }
              Tray tray = trays.get(trayId);
              if (tray == null) {
                tray =
                    new Tray(
                        new TrayId(trayId),
                        row.getString("tray_name"),
                        row.getInt("tray_position"));
                trays.put(trayId, tray);
                zone.add(tray);
              }
              UUID itemId = row.getObject("item_id", UUID.class);
              if (itemId != null) {
                items.add(
                    new Object[] {
                      tray,
                      itemId,
                      row.getObject("food_id", UUID.class),
                      row.getBigDecimal("quantity_g"),
                      row.getBigDecimal("tare_g"),
                      row.getObject("expires_on", LocalDate.class)
                    });
              }
            });
    if (!items.isEmpty()) {
      Set<UUID> foodIds = new HashSet<>();
      items.forEach(item -> foodIds.add((UUID) item[2]));
      Map<UUID, FoodMetadata> foods =
          FoodRows.load(jdbc, "WHERE id IN (:ids)", Map.of("ids", foodIds));
      for (Object[] item : items) {
        ((Tray) item[0])
            .add(
                new FoodItem(
                    new FoodItemId((UUID) item[1]),
                    foods.get((UUID) item[2]),
                    Grams.of((BigDecimal) item[3]),
                    Grams.of((BigDecimal) item[4]),
                    (LocalDate) item[5]));
      }
    }
    return List.copyOf(fridges.values());
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
    List<Object[]> zoneRows = new ArrayList<>();
    List<Object[]> trayRows = new ArrayList<>();
    List<FoodItem> foodItems = new ArrayList<>();
    List<Object[]> itemPlacement = new ArrayList<>();
    List<Zone> fridgeZones = fridge.children();
    for (int zoneIndex = 0; zoneIndex < fridgeZones.size(); zoneIndex++) {
      Zone zone = fridgeZones.get(zoneIndex);
      zoneRows.add(
          new Object[] {zone.id().value(), fridgeId, zone.name(), zone.kind().name(), zoneIndex});
      for (Tray tray : zone.children()) {
        trayRows.add(
            new Object[] {tray.id().value(), zone.id().value(), tray.name(), tray.position()});
        List<FoodItem> trayItems = tray.children();
        for (int itemIndex = 0; itemIndex < trayItems.size(); itemIndex++) {
          foodItems.add(trayItems.get(itemIndex));
          itemPlacement.add(new Object[] {tray.id().value(), itemIndex});
        }
      }
    }
    batch.batchUpdate(
        """
        INSERT INTO zones (id, fridge_id, name, kind, position) VALUES (?, ?, ?, ?, ?)
        ON CONFLICT (id) DO UPDATE SET
            name = EXCLUDED.name, kind = EXCLUDED.kind, position = EXCLUDED.position
        """,
        zoneRows);
    batch.batchUpdate(
        """
        INSERT INTO trays (id, zone_id, name, position) VALUES (?, ?, ?, ?)
        ON CONFLICT (id) DO UPDATE SET
            zone_id = EXCLUDED.zone_id, name = EXCLUDED.name, position = EXCLUDED.position
        """,
        trayRows);
    Map<String, UUID> foodIds = foodIds(foodItems);
    List<Object[]> itemRows = new ArrayList<>();
    for (int index = 0; index < foodItems.size(); index++) {
      FoodItem item = foodItems.get(index);
      UUID foodId = foodIds.get(item.food().key());
      if (foodId == null) {
        throw new IllegalStateException("Food not in catalog: " + item.food().name());
      }
      itemRows.add(
          new Object[] {
            item.id().value(),
            householdId,
            itemPlacement.get(index)[0],
            foodId,
            item.quantity().value(),
            item.tare().value(),
            item.expiresOn().map(java.sql.Date::valueOf).orElse(null),
            itemPlacement.get(index)[1]
          });
    }
    batch.batchUpdate(
        """
        INSERT INTO food_items (id, household_id, tray_id, food_id, quantity_g, tare_g,
                                expires_on, position)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (id) DO UPDATE SET
            tray_id = EXCLUDED.tray_id,
            quantity_g = EXCLUDED.quantity_g,
            expires_on = EXCLUDED.expires_on,
            position = EXCLUDED.position,
            version = food_items.version + 1
        """,
        itemRows,
        new int[] {
          Types.OTHER,
          Types.OTHER,
          Types.OTHER,
          Types.OTHER,
          Types.NUMERIC,
          Types.NUMERIC,
          Types.DATE,
          Types.INTEGER
        });
    deleteMissing(
        """
        DELETE FROM food_items i USING trays t, zones z
        WHERE i.tray_id = t.id AND t.zone_id = z.id AND z.fridge_id = ?
          AND NOT (i.id = ANY (?))
        """,
        fridgeId,
        foodItems.stream().map(item -> item.id().value()).toList());
    deleteMissing(
        """
        DELETE FROM trays t USING zones z
        WHERE t.zone_id = z.id AND z.fridge_id = ? AND NOT (t.id = ANY (?))
        """,
        fridgeId,
        trayRows.stream().map(row -> (UUID) row[0]).toList());
    deleteMissing(
        "DELETE FROM zones WHERE fridge_id = ? AND NOT (id = ANY (?))",
        fridgeId,
        zoneRows.stream().map(row -> (UUID) row[0]).toList());
  }

  private Map<String, UUID> foodIds(List<FoodItem> items) {
    Map<String, UUID> ids = new HashMap<>();
    if (items.isEmpty()) {
      return ids;
    }
    Set<String> keys = new HashSet<>();
    items.forEach(item -> keys.add(item.food().key()));
    jdbc.sql("SELECT id, name_key FROM food_catalog WHERE name_key IN (:keys)")
        .param("keys", keys)
        .query(
            row -> {
              ids.put(row.getString("name_key"), row.getObject("id", UUID.class));
            });
    return ids;
  }

  private void deleteMissing(String sql, UUID fridgeId, List<UUID> keep) {
    batch.update(
        connection -> {
          var statement = connection.prepareStatement(sql);
          statement.setObject(1, fridgeId);
          statement.setArray(2, connection.createArrayOf("uuid", keep.toArray()));
          return statement;
        });
  }
}
