package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.port.ShelfLifeCatalog;
import dev.haypacomer.domain.expiry.ShelfLife;
import dev.haypacomer.domain.food.FoodMetadata;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresShelfLifeCatalog implements ShelfLifeCatalog {

  private final JdbcClient jdbc;

  public PostgresShelfLifeCatalog(DataSource dataSource) {
    this.jdbc = JdbcClient.create(dataSource);
  }

  @Override
  public ShelfLife shelfLife(FoodMetadata food) {
    return jdbc.sql(
            """
            SELECT fridge_days, door_days, freezer_days, opened_days FROM food_catalog
            WHERE name_key = :key AND fridge_days IS NOT NULL AND door_days IS NOT NULL
              AND freezer_days IS NOT NULL AND opened_days IS NOT NULL
            """)
        .param("key", food.key())
        .query(
            (row, number) ->
                new ShelfLife(
                    row.getInt("fridge_days"),
                    row.getInt("door_days"),
                    row.getInt("freezer_days"),
                    row.getInt("opened_days")))
        .optional()
        .orElseGet(() -> ShelfLife.of(food));
  }
}
