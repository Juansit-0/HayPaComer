package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.port.SubstitutionRuleRepository;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.substitution.SubstitutionRule;
import dev.haypacomer.domain.substitution.SubstitutionRuleId;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresSubstitutionRuleRepository implements SubstitutionRuleRepository {

  private final JdbcClient jdbc;

  public PostgresSubstitutionRuleRepository(DataSource dataSource) {
    this.jdbc = JdbcClient.create(dataSource);
  }

  @Override
  public void save(SubstitutionRule rule) {
    jdbc.sql(
            """
            INSERT INTO substitution_rules (id, from_food_id, to_food_id, ratio, max_g, position)
            SELECT :id, f.id, t.id, :ratio, :maxGrams,
                   COALESCE((SELECT max(position) FROM substitution_rules), 0) + 1
            FROM food_catalog f, food_catalog t
            WHERE f.name_key = :fromKey AND t.name_key = :toKey
            ON CONFLICT (id) DO UPDATE SET ratio = EXCLUDED.ratio, max_g = EXCLUDED.max_g
            """)
        .param("id", rule.id().value())
        .param("ratio", rule.ratio())
        .param("maxGrams", rule.maxReplaced().value())
        .param("fromKey", rule.original().key())
        .param("toKey", rule.substitute().key())
        .update();
  }

  @Override
  public List<SubstitutionRule> all() {
    List<RuleRow> rows =
        jdbc.sql(
                "SELECT id, from_food_id, to_food_id, ratio, max_g FROM substitution_rules"
                    + " ORDER BY position")
            .query(
                (row, rowNumber) ->
                    new RuleRow(
                        row.getObject("id", UUID.class),
                        row.getObject("from_food_id", UUID.class),
                        row.getObject("to_food_id", UUID.class),
                        row.getBigDecimal("ratio"),
                        row.getBigDecimal("max_g")))
            .list();
    if (rows.isEmpty()) {
      return List.of();
    }
    Set<UUID> foodIds = new HashSet<>();
    rows.forEach(
        row -> {
          foodIds.add(row.from());
          foodIds.add(row.to());
        });
    Map<UUID, FoodMetadata> foods =
        FoodRows.load(jdbc, "WHERE id IN (:ids)", Map.of("ids", foodIds));
    return rows.stream()
        .map(
            row ->
                new SubstitutionRule(
                    new SubstitutionRuleId(row.id()),
                    foods.get(row.from()),
                    foods.get(row.to()),
                    row.ratio().stripTrailingZeros(),
                    Grams.of(row.maxGrams())))
        .toList();
  }

  private record RuleRow(UUID id, UUID from, UUID to, BigDecimal ratio, BigDecimal maxGrams) {}
}
