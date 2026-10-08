package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.port.RecipeRepository;
import dev.haypacomer.application.port.RecipeTemplateRepository;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.recipe.ClonedRecipe;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeId;
import dev.haypacomer.domain.recipe.RecipeRequirement;
import dev.haypacomer.domain.recipe.RecipeSource;
import dev.haypacomer.domain.recipe.RecipeStep;
import dev.haypacomer.domain.recipe.StepWeighing;
import java.math.BigDecimal;
import java.sql.Types;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class PostgresRecipeRepository implements RecipeRepository, RecipeTemplateRepository {

  private final JdbcClient jdbc;
  private final TransactionTemplate transaction;

  public PostgresRecipeRepository(DataSource dataSource) {
    this.jdbc = JdbcClient.create(dataSource);
    this.transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
  }

  @Override
  public void save(HouseholdId household, Recipe recipe) {
    transaction.executeWithoutResult(status -> write(household, recipe, null));
  }

  @Override
  public void saveCopy(HouseholdId household, ClonedRecipe copy) {
    transaction.executeWithoutResult(
        status -> write(household, copy.recipe(), copy.clonedFrom().value()));
  }

  @Override
  public List<Recipe> templates() {
    return load(
        null,
        jdbc.sql("SELECT id FROM recipes WHERE is_template ORDER BY name")
            .query(UUID.class)
            .list());
  }

  @Override
  public Optional<Recipe> template(RecipeId id) {
    return jdbc.sql("SELECT id FROM recipes WHERE id = :id AND is_template")
        .param("id", id.value())
        .query(UUID.class)
        .optional()
        .flatMap(found -> load(null, List.of(found)).stream().findFirst());
  }

  @Override
  public Optional<Recipe> find(HouseholdId household, RecipeId id) {
    return load(household, List.of(id.value())).stream().findFirst();
  }

  @Override
  public List<Recipe> findByHousehold(HouseholdId household) {
    return load(
        household,
        jdbc.sql("SELECT id FROM recipes WHERE household_id = :household ORDER BY created_at, id")
            .param("household", household.value())
            .query(UUID.class)
            .list());
  }

  List<Recipe> loadByIds(Collection<UUID> ids) {
    return load(null, ids);
  }

  private List<Recipe> load(HouseholdId household, Collection<UUID> ids) {
    if (ids.isEmpty()) {
      return List.of();
    }
    Map<UUID, RecipeRow> rows = new LinkedHashMap<>();
    jdbc
        .sql(
            "SELECT id, household_id, name, servings, minutes, source FROM recipes"
                + " WHERE id IN (:ids)")
        .param("ids", ids)
        .query(
            (row, rowNumber) ->
                new RecipeRow(
                    row.getObject("id", UUID.class),
                    row.getObject("household_id", UUID.class),
                    row.getString("name"),
                    row.getInt("servings"),
                    row.getInt("minutes"),
                    RecipeSource.valueOf(row.getString("source"))))
        .list()
        .stream()
        .filter(row -> household == null || household.value().equals(row.household()))
        .forEach(row -> rows.put(row.id(), row));
    if (rows.isEmpty()) {
      return List.of();
    }
    List<RequirementRow> requirements =
        jdbc.sql(
                "SELECT recipe_id, food_id, grams, optional FROM recipe_requirements"
                    + " WHERE recipe_id IN (:ids) ORDER BY position")
            .param("ids", rows.keySet())
            .query(
                (row, rowNumber) ->
                    new RequirementRow(
                        row.getObject("recipe_id", UUID.class),
                        row.getObject("food_id", UUID.class),
                        row.getBigDecimal("grams"),
                        row.getBoolean("optional")))
            .list();
    List<StepRow> steps =
        jdbc.sql(
                "SELECT recipe_id, position, instruction, timer_seconds, weigh_food_id,"
                    + " weigh_grams FROM recipe_steps WHERE recipe_id IN (:ids) ORDER BY position")
            .param("ids", rows.keySet())
            .query(
                (row, rowNumber) ->
                    new StepRow(
                        row.getObject("recipe_id", UUID.class),
                        row.getInt("position"),
                        row.getString("instruction"),
                        (Long) row.getObject("timer_seconds"),
                        row.getObject("weigh_food_id", UUID.class),
                        row.getBigDecimal("weigh_grams")))
            .list();
    Set<UUID> foodIds = new HashSet<>();
    requirements.forEach(requirement -> foodIds.add(requirement.food()));
    steps.stream()
        .filter(step -> step.weighFood() != null)
        .forEach(step -> foodIds.add(step.weighFood()));
    Map<UUID, FoodMetadata> foods =
        FoodRows.load(jdbc, "WHERE id IN (:ids)", Map.of("ids", foodIds));
    List<Recipe> recipes = new ArrayList<>();
    for (RecipeRow row : rows.values()) {
      recipes.add(
          new Recipe(
              new RecipeId(row.id()),
              row.name(),
              row.servings(),
              row.minutes(),
              row.source(),
              requirements.stream()
                  .filter(requirement -> requirement.recipe().equals(row.id()))
                  .map(
                      requirement ->
                          new RecipeRequirement(
                              foods.get(requirement.food()),
                              Grams.of(requirement.grams()),
                              requirement.optional()))
                  .toList(),
              steps.stream()
                  .filter(step -> step.recipe().equals(row.id()))
                  .map(step -> step.toStep(foods))
                  .toList()));
    }
    return recipes;
  }

  private void write(HouseholdId household, Recipe recipe, UUID clonedFrom) {
    jdbc.sql(
            """
            INSERT INTO recipes (id, household_id, name, servings, minutes, source, cloned_from)
            VALUES (:id, :household, :name, :servings, :minutes, :source, :clonedFrom)
            ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, servings = EXCLUDED.servings,
                minutes = EXCLUDED.minutes, source = EXCLUDED.source
            """)
        .param("id", recipe.id().value())
        .param("household", household.value())
        .param("name", recipe.name())
        .param("servings", recipe.servings())
        .param("minutes", recipe.minutes())
        .param("source", recipe.source().name())
        .param("clonedFrom", clonedFrom, Types.OTHER)
        .update();
    jdbc.sql("DELETE FROM recipe_requirements WHERE recipe_id = :id")
        .param("id", recipe.id().value())
        .update();
    jdbc.sql("DELETE FROM recipe_steps WHERE recipe_id = :id")
        .param("id", recipe.id().value())
        .update();
    int position = 0;
    for (RecipeRequirement requirement : recipe.requirements()) {
      jdbc.sql(
              """
              INSERT INTO recipe_requirements (recipe_id, food_id, position, grams, optional)
              SELECT :recipe, id, :position, :grams, :optional FROM food_catalog
              WHERE name_key = :food
              """)
          .param("recipe", recipe.id().value())
          .param("position", position++)
          .param("grams", requirement.grams().value())
          .param("optional", requirement.optional())
          .param("food", requirement.food().key())
          .update();
    }
    for (RecipeStep step : recipe.steps()) {
      jdbc.sql(
              """
              INSERT INTO recipe_steps (recipe_id, position, instruction, timer_seconds,
                                        weigh_food_id, weigh_grams)
              VALUES (:recipe, :position, :instruction, :timer,
                      (SELECT id FROM food_catalog WHERE name_key = :weighFood), :weighGrams)
              """)
          .param("recipe", recipe.id().value())
          .param("position", step.position())
          .param("instruction", step.instruction())
          .param("timer", step.timerDuration().map(Duration::toSeconds).orElse(null), Types.BIGINT)
          .param(
              "weighFood",
              step.weighingTarget().map(weighing -> weighing.food().key()).orElse(null),
              Types.VARCHAR)
          .param(
              "weighGrams",
              step.weighingTarget().map(weighing -> weighing.target().value()).orElse(null),
              Types.NUMERIC)
          .update();
    }
  }

  private record RecipeRow(
      UUID id, UUID household, String name, int servings, int minutes, RecipeSource source) {}

  private record RequirementRow(UUID recipe, UUID food, BigDecimal grams, boolean optional) {}

  private record StepRow(
      UUID recipe,
      int position,
      String instruction,
      Long timerSeconds,
      UUID weighFood,
      BigDecimal weighGrams) {

    RecipeStep toStep(Map<UUID, FoodMetadata> foods) {
      RecipeStep step = RecipeStep.of(position, instruction);
      if (timerSeconds != null) {
        step = step.withTimer(Duration.ofSeconds(timerSeconds));
      }
      if (weighFood != null) {
        step = step.withWeighing(new StepWeighing(foods.get(weighFood), Grams.of(weighGrams)));
      }
      return step;
    }
  }
}
