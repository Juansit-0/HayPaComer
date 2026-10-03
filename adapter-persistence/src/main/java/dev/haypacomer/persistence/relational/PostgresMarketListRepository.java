package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.port.MarketListRepository;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.market.MarketItem;
import dev.haypacomer.domain.market.MarketItemId;
import dev.haypacomer.domain.market.MarketList;
import dev.haypacomer.domain.market.MarketSource;
import dev.haypacomer.domain.quantity.Grams;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class PostgresMarketListRepository implements MarketListRepository {

  private final JdbcClient jdbc;
  private final TransactionTemplate transaction;

  public PostgresMarketListRepository(DataSource dataSource) {
    this.jdbc = JdbcClient.create(dataSource);
    this.transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
  }

  @Override
  public void save(MarketList list) {
    transaction.executeWithoutResult(status -> write(list));
  }

  @Override
  public Optional<MarketList> findByHousehold(HouseholdId household) {
    Map<UUID, FoodMetadata> foods =
        FoodRows.load(
            jdbc,
            "WHERE id IN (SELECT food_id FROM market_items WHERE household_id = :household)",
            Map.of("household", household.value()));
    List<MarketItem> items =
        jdbc.sql(
                """
                SELECT id, food_id, grams_needed, source, added_by, added_at, checked_at
                FROM market_items WHERE household_id = :household ORDER BY position, id
                """)
            .param("household", household.value())
            .query(
                (row, rowNumber) -> {
                  OffsetDateTime checkedAt = row.getObject("checked_at", OffsetDateTime.class);
                  UUID addedBy = row.getObject("added_by", UUID.class);
                  return new MarketItem(
                      new MarketItemId(row.getObject("id", UUID.class)),
                      foods.get(row.getObject("food_id", UUID.class)),
                      Grams.of(row.getBigDecimal("grams_needed")),
                      MarketSource.valueOf(row.getString("source")),
                      new UserId(addedBy == null ? new UUID(0, 0) : addedBy),
                      Timestamps.read(row, "added_at"),
                      checkedAt == null ? null : checkedAt.toInstant());
                })
            .list();
    return items.isEmpty() ? Optional.empty() : Optional.of(MarketList.restore(household, items));
  }

  private void write(MarketList list) {
    UUID household = list.household().value();
    jdbc.sql("DELETE FROM market_items WHERE household_id = :household")
        .param("household", household)
        .update();
    List<MarketItem> items = list.items();
    for (int position = 0; position < items.size(); position++) {
      MarketItem item = items.get(position);
      UUID food =
          jdbc.sql("SELECT id FROM food_catalog WHERE name_key = :key")
              .param("key", item.food().key())
              .query(UUID.class)
              .optional()
              .orElseThrow(
                  () -> new IllegalStateException("Food not in catalog: " + item.food().name()));
      jdbc.sql(
              """
              INSERT INTO market_items (id, household_id, food_id, grams_needed, source, added_by,
                                        added_at, checked_at, position)
              VALUES (:id, :household, :food, :grams, :source,
                      (SELECT id FROM users WHERE id = :addedBy), :addedAt, :checkedAt, :position)
              """)
          .param("id", item.id().value())
          .param("household", household)
          .param("food", food)
          .param("grams", item.grams().value())
          .param("source", item.source().name())
          .param("addedBy", item.addedBy().value())
          .param("addedAt", Timestamps.toDatabase(item.addedAt()))
          .param(
              "checkedAt",
              item.checkedAt() == null ? null : Timestamps.toDatabase(item.checkedAt()),
              Types.TIMESTAMP_WITH_TIMEZONE)
          .param("position", position)
          .update();
    }
  }
}
