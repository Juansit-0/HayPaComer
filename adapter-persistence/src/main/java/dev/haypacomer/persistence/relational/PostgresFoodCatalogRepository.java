package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.port.FoodCatalogRepository;
import dev.haypacomer.domain.food.Allergen;
import dev.haypacomer.domain.food.FoodMetadata;
import java.text.Normalizer;
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
public class PostgresFoodCatalogRepository implements FoodCatalogRepository {

  private final JdbcClient jdbc;
  private final TransactionTemplate transaction;

  public PostgresFoodCatalogRepository(DataSource dataSource) {
    this.jdbc = JdbcClient.create(dataSource);
    this.transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
  }

  @Override
  public void save(FoodMetadata food) {
    transaction.executeWithoutResult(status -> write(food));
  }

  @Override
  public Optional<FoodMetadata> findByName(String name) {
    return FoodRows.load(jdbc, "WHERE name_key = :key", Map.of("key", FoodMetadata.keyOf(name)))
        .values()
        .stream()
        .findFirst();
  }

  @Override
  public List<FoodMetadata> search(String text, int limit) {
    if (limit < 1) {
      throw new IllegalArgumentException("Limit must be positive: " + limit);
    }
    String prefix =
        FoodMetadata.keyOf(plain(text))
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    return List.copyOf(
        FoodRows.load(
                jdbc,
                "WHERE name_key LIKE :prefix OR name_key IN (SELECT substr(t.key, 6) FROM"
                    + " translations t WHERE t.key LIKE 'food.%' AND translate(lower(t.text),"
                    + " 'áéíóúüñ', 'aeiouun') LIKE :prefix) ORDER BY name_key LIMIT :limit",
                Map.of("prefix", prefix + "%", "limit", limit))
            .values());
  }

  private static String plain(String text) {
    return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
  }

  private void write(FoodMetadata food) {
    UUID id =
        jdbc.sql("SELECT id FROM food_catalog WHERE name_key = :key")
            .param("key", food.key())
            .query(UUID.class)
            .optional()
            .orElseGet(UUID::randomUUID);
    jdbc.sql(
            """
            INSERT INTO food_catalog (id, name, name_key, category, default_unit, grams_per_ml,
                                      grams_per_piece, perishable, shelf_days)
            VALUES (:id, :name, :key, :category, :unit, :density, :piece, :perishable, :shelfDays)
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                category = EXCLUDED.category,
                default_unit = EXCLUDED.default_unit,
                grams_per_ml = EXCLUDED.grams_per_ml,
                grams_per_piece = EXCLUDED.grams_per_piece,
                perishable = EXCLUDED.perishable,
                shelf_days = EXCLUDED.shelf_days
            """)
        .param("id", id)
        .param("name", food.name())
        .param("key", food.key())
        .param("category", food.category().name())
        .param("unit", food.defaultUnit().name())
        .param("density", food.conversion().gramsPerMilliliter())
        .param("piece", food.conversion().gramsPerPiece())
        .param("perishable", food.perishable())
        .param("shelfDays", food.shelfDays())
        .update();
    jdbc.sql("DELETE FROM food_allergens WHERE food_id = :id").param("id", id).update();
    for (Allergen allergen : food.allergens()) {
      jdbc.sql(
              """
              INSERT INTO food_allergens (food_id, allergen_id)
              SELECT :id, id FROM allergens WHERE code = :code
              """)
          .param("id", id)
          .param("code", allergen.name())
          .update();
    }
  }
}
