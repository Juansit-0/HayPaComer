package dev.haypacomer.persistence.relational;

import dev.haypacomer.domain.food.Allergen;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Unit;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

final class FoodRows {

  static final String COLUMNS =
      "id, name, category, default_unit, grams_per_ml, grams_per_piece, perishable, shelf_days";

  private FoodRows() {}

  static Map<UUID, FoodMetadata> load(JdbcClient jdbc, String whereClause, Map<String, ?> params) {
    Map<UUID, FoodRow> rows = new LinkedHashMap<>();
    jdbc.sql("SELECT " + COLUMNS + " FROM food_catalog " + whereClause)
        .params(params)
        .query(
            (row, rowNumber) ->
                new FoodRow(
                    row.getObject("id", UUID.class),
                    row.getString("name"),
                    FoodCategory.valueOf(row.getString("category")),
                    Unit.valueOf(row.getString("default_unit")),
                    row.getBigDecimal("grams_per_ml"),
                    row.getBigDecimal("grams_per_piece"),
                    row.getBoolean("perishable"),
                    row.getInt("shelf_days")))
        .list()
        .forEach(row -> rows.put(row.id(), row));
    Map<UUID, Set<Allergen>> allergens = allergensOf(jdbc, rows.keySet());
    Map<UUID, FoodMetadata> foods = new LinkedHashMap<>();
    rows.values()
        .forEach(
            row -> foods.put(row.id(), row.toMetadata(allergens.getOrDefault(row.id(), Set.of()))));
    return foods;
  }

  private static Map<UUID, Set<Allergen>> allergensOf(JdbcClient jdbc, Collection<UUID> foodIds) {
    Map<UUID, Set<Allergen>> allergens = new HashMap<>();
    if (foodIds.isEmpty()) {
      return allergens;
    }
    jdbc.sql(
            """
            SELECT fa.food_id, a.code FROM food_allergens fa
            JOIN allergens a ON a.id = fa.allergen_id
            WHERE fa.food_id IN (:ids)
            """)
        .param("ids", foodIds)
        .query(
            (row, rowNumber) -> {
              allergens
                  .computeIfAbsent(
                      row.getObject("food_id", UUID.class), id -> EnumSet.noneOf(Allergen.class))
                  .add(Allergen.valueOf(row.getString("code")));
              return row.getString("code");
            })
        .list();
    return allergens;
  }

  private record FoodRow(
      UUID id,
      String name,
      FoodCategory category,
      Unit defaultUnit,
      BigDecimal gramsPerMilliliter,
      BigDecimal gramsPerPiece,
      boolean perishable,
      int shelfDays) {

    FoodMetadata toMetadata(Set<Allergen> allergens) {
      return new FoodMetadata(
          name,
          category,
          defaultUnit,
          new ConversionFactors(gramsPerMilliliter, gramsPerPiece),
          perishable,
          shelfDays,
          allergens);
    }
  }
}
