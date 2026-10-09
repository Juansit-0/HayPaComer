package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.port.FoodPriceRepository;
import dev.haypacomer.domain.household.HouseholdId;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresFoodPriceRepository implements FoodPriceRepository {

  static final Currency REFERENCE_CURRENCY = Currency.getInstance("COP");

  private final JdbcClient jdbc;

  public PostgresFoodPriceRepository(DataSource dataSource) {
    this.jdbc = JdbcClient.create(dataSource);
  }

  @Override
  public Map<String, BigDecimal> pricesFor(HouseholdId household, Currency currency) {
    Map<String, BigDecimal> prices = new HashMap<>();
    if (REFERENCE_CURRENCY.equals(currency)) {
      jdbc.sql(
              "SELECT name_key, reference_price_cop_per_kg FROM food_catalog"
                  + " WHERE reference_price_cop_per_kg IS NOT NULL")
          .query(
              (row, number) ->
                  prices.put(
                      row.getString("name_key"), row.getBigDecimal("reference_price_cop_per_kg")))
          .list();
    }
    jdbc.sql("SELECT food_key, price_per_kg FROM food_prices WHERE household_id = :household")
        .param("household", household.value())
        .query(
            (row, number) ->
                prices.put(row.getString("food_key"), row.getBigDecimal("price_per_kg")))
        .list();
    return prices;
  }

  @Override
  public void save(HouseholdId household, String foodKey, BigDecimal pricePerKg) {
    jdbc.sql(
            """
            INSERT INTO food_prices (household_id, food_key, price_per_kg, updated_at)
            VALUES (:household, :food, :price, now())
            ON CONFLICT (household_id, food_key)
            DO UPDATE SET price_per_kg = EXCLUDED.price_per_kg, updated_at = now()
            """)
        .param("household", household.value())
        .param("food", foodKey)
        .param("price", pricePerKg)
        .update();
  }
}
