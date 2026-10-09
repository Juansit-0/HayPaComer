package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.port.UnitOfWork;
import dev.haypacomer.domain.household.HouseholdId;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class PostgresUnitOfWork implements UnitOfWork {

  private final TransactionTemplate transaction;
  private final JdbcClient jdbc;

  public PostgresUnitOfWork(DataSource dataSource) {
    this.transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    this.jdbc = JdbcClient.create(dataSource);
  }

  @Override
  public <T> T run(Supplier<T> work) {
    return transaction.execute(status -> work.get());
  }

  @Override
  public <T> T runFor(HouseholdId household, Supplier<T> work) {
    return transaction.execute(
        status -> {
          jdbc.sql("SELECT pg_advisory_xact_lock(hashtextextended(:household, 0))")
              .param("household", household.value().toString())
              .query()
              .singleValue();
          return work.get();
        });
  }
}
