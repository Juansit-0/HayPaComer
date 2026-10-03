package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.port.FoodOwnershipRepository;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.inventory.Ownership;
import dev.haypacomer.domain.inventory.Visibility;
import dev.haypacomer.domain.member.MemberId;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class PostgresFoodOwnershipRepository implements FoodOwnershipRepository {

  private final JdbcClient jdbc;
  private final TransactionTemplate transaction;

  public PostgresFoodOwnershipRepository(DataSource dataSource) {
    this.jdbc = JdbcClient.create(dataSource);
    this.transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
  }

  @Override
  public void save(FoodItemId item, Ownership ownership) {
    transaction.executeWithoutResult(
        status -> {
          int updated =
              jdbc.sql(
                      "UPDATE food_items SET owner_member_id = :owner, visibility = :visibility"
                          + " WHERE id = :id")
                  .param("owner", ownership.owner().value())
                  .param("visibility", ownership.visibility().name())
                  .param("id", item.value())
                  .update();
          if (updated == 0) {
            throw new IllegalStateException("Food item not stored: " + item.value());
          }
          jdbc.sql("DELETE FROM food_access_grants WHERE food_item_id = :id")
              .param("id", item.value())
              .update();
          for (MemberId grantee : ownership.grantees()) {
            jdbc.sql(
                    """
                    INSERT INTO food_access_grants (food_item_id, grantee_member_id, granted_by)
                    VALUES (:id, :grantee, :owner)
                    """)
                .param("id", item.value())
                .param("grantee", grantee.value())
                .param("owner", ownership.owner().value())
                .update();
          }
        });
  }

  @Override
  public Optional<Ownership> find(FoodItemId item) {
    return jdbc.sql(
            "SELECT owner_member_id, visibility FROM food_items"
                + " WHERE id = :id AND owner_member_id IS NOT NULL")
        .param("id", item.value())
        .query(
            (row, rowNumber) ->
                new Ownership(
                    new MemberId(row.getObject("owner_member_id", UUID.class)),
                    Visibility.valueOf(row.getString("visibility")),
                    new HashSet<>(
                        jdbc
                            .sql(
                                "SELECT grantee_member_id FROM food_access_grants"
                                    + " WHERE food_item_id = :id")
                            .param("id", item.value())
                            .query(UUID.class)
                            .list()
                            .stream()
                            .map(MemberId::new)
                            .toList())))
        .optional();
  }
}
