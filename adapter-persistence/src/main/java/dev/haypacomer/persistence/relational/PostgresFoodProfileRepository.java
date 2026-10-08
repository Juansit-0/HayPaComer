package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.port.FoodProfileRepository;
import dev.haypacomer.domain.food.Allergen;
import dev.haypacomer.domain.member.Diet;
import dev.haypacomer.domain.member.FoodProfile;
import dev.haypacomer.domain.member.MemberId;
import java.util.HashSet;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class PostgresFoodProfileRepository implements FoodProfileRepository {

  private final JdbcClient jdbc;
  private final TransactionTemplate transaction;

  public PostgresFoodProfileRepository(DataSource dataSource) {
    this.jdbc = JdbcClient.create(dataSource);
    this.transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
  }

  @Override
  public void save(FoodProfile profile) {
    transaction.executeWithoutResult(status -> write(profile));
  }

  @Override
  public Optional<FoodProfile> find(MemberId member) {
    return jdbc.sql("SELECT diet FROM member_profiles WHERE member_id = :member")
        .param("member", member.value())
        .query(String.class)
        .optional()
        .map(
            diet ->
                new FoodProfile(
                    member,
                    Diet.valueOf(diet),
                    new HashSet<>(
                        jdbc.sql(
                                """
                                SELECT a.code FROM profile_allergens pa
                                JOIN allergens a ON a.id = pa.allergen_id
                                WHERE pa.member_id = :member
                                """)
                            .param("member", member.value())
                            .query((row, rowNumber) -> Allergen.valueOf(row.getString("code")))
                            .list()),
                    new HashSet<>(
                        jdbc.sql(
                                "SELECT food_key FROM profile_avoided_foods"
                                    + " WHERE member_id = :member")
                            .param("member", member.value())
                            .query(String.class)
                            .list())));
  }

  private void write(FoodProfile profile) {
    jdbc.sql(
            """
            INSERT INTO member_profiles (member_id, diet) VALUES (:member, :diet)
            ON CONFLICT (member_id) DO UPDATE SET diet = EXCLUDED.diet
            """)
        .param("member", profile.member().value())
        .param("diet", profile.diet().name())
        .update();
    jdbc.sql("DELETE FROM profile_allergens WHERE member_id = :member")
        .param("member", profile.member().value())
        .update();
    for (Allergen allergen : profile.allergies()) {
      jdbc.sql(
              """
              INSERT INTO profile_allergens (member_id, allergen_id)
              SELECT :member, id FROM allergens WHERE code = :code
              """)
          .param("member", profile.member().value())
          .param("code", allergen.name())
          .update();
    }
    jdbc.sql("DELETE FROM profile_avoided_foods WHERE member_id = :member")
        .param("member", profile.member().value())
        .update();
    for (String food : profile.avoidedFoods()) {
      jdbc.sql("INSERT INTO profile_avoided_foods (member_id, food_key) VALUES (:member, :food)")
          .param("member", profile.member().value())
          .param("food", food)
          .update();
    }
  }
}
