package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.port.MarketBudgetRepository;
import dev.haypacomer.domain.household.HouseholdId;
import java.math.BigDecimal;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresMarketBudgetRepository implements MarketBudgetRepository {

  private final JdbcClient jdbc;

  public PostgresMarketBudgetRepository(DataSource dataSource) {
    this.jdbc = JdbcClient.create(dataSource);
  }

  @Override
  public Optional<BigDecimal> monthly(HouseholdId household) {
    return jdbc.sql("SELECT monthly_amount FROM market_budgets WHERE household_id = :household")
        .param("household", household.value())
        .query(BigDecimal.class)
        .optional();
  }

  @Override
  public void save(HouseholdId household, BigDecimal monthly) {
    jdbc.sql(
            """
            INSERT INTO market_budgets (household_id, monthly_amount, updated_at)
            VALUES (:household, :amount, now())
            ON CONFLICT (household_id)
            DO UPDATE SET monthly_amount = EXCLUDED.monthly_amount, updated_at = now()
            """)
        .param("household", household.value())
        .param("amount", monthly)
        .update();
  }
}
