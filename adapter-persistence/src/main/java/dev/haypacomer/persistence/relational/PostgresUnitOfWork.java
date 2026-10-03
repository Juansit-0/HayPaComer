package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.port.UnitOfWork;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class PostgresUnitOfWork implements UnitOfWork {

  private final TransactionTemplate transaction;

  public PostgresUnitOfWork(DataSource dataSource) {
    this.transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
  }

  @Override
  public <T> T run(Supplier<T> work) {
    return transaction.execute(status -> work.get());
  }
}
