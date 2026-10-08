package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.port.WeeklyPlanRepository;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.planning.Meal;
import dev.haypacomer.domain.planning.PlanEntry;
import dev.haypacomer.domain.planning.PlanEntryId;
import dev.haypacomer.domain.planning.WeeklyPlan;
import dev.haypacomer.domain.planning.WeeklyPlanId;
import dev.haypacomer.domain.recipe.Recipe;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
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
public class PostgresWeeklyPlanRepository implements WeeklyPlanRepository {

  private final JdbcClient jdbc;
  private final TransactionTemplate transaction;
  private final PostgresRecipeRepository recipes;

  public PostgresWeeklyPlanRepository(DataSource dataSource) {
    this.jdbc = JdbcClient.create(dataSource);
    this.transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    this.recipes = new PostgresRecipeRepository(dataSource);
  }

  @Override
  public void save(WeeklyPlan plan) {
    transaction.executeWithoutResult(status -> write(plan));
  }

  @Override
  public Optional<WeeklyPlan> current(HouseholdId household, LocalDate today) {
    return jdbc.sql(
            """
            SELECT id FROM weekly_plans
            WHERE household_id = :household AND week_start <= :today
              AND week_start > :today - 7
            ORDER BY week_start DESC LIMIT 1
            """)
        .param("household", household.value())
        .param("today", today)
        .query(UUID.class)
        .optional()
        .map(id -> load(household, id));
  }

  @Override
  public Optional<WeeklyPlan> findByEntry(HouseholdId household, PlanEntryId entry) {
    return jdbc.sql(
            """
            SELECT p.id FROM weekly_plans p JOIN plan_entries e ON e.plan_id = p.id
            WHERE p.household_id = :household AND e.id = :entry
            """)
        .param("household", household.value())
        .param("entry", entry.value())
        .query(UUID.class)
        .optional()
        .map(id -> load(household, id));
  }

  private WeeklyPlan load(HouseholdId household, UUID id) {
    LocalDate weekStart =
        jdbc.sql("SELECT week_start FROM weekly_plans WHERE id = :id")
            .param("id", id)
            .query(LocalDate.class)
            .single();
    List<EntryRow> rows =
        jdbc.sql(
                "SELECT id, day, meal, recipe_id, servings, needs_shopping FROM plan_entries"
                    + " WHERE plan_id = :id")
            .param("id", id)
            .query(
                (row, rowNumber) ->
                    new EntryRow(
                        row.getObject("id", UUID.class),
                        row.getInt("day"),
                        Meal.valueOf(row.getString("meal")),
                        row.getObject("recipe_id", UUID.class),
                        row.getInt("servings"),
                        row.getBoolean("needs_shopping")))
            .list();
    Set<UUID> recipeIds = new HashSet<>();
    rows.forEach(row -> recipeIds.add(row.recipe()));
    Map<UUID, Recipe> byId = new HashMap<>();
    recipes.loadByIds(recipeIds).forEach(recipe -> byId.put(recipe.id().value(), recipe));
    return WeeklyPlan.restore(
        new WeeklyPlanId(id),
        household,
        weekStart,
        rows.stream()
            .map(
                row ->
                    new PlanEntry(
                        new PlanEntryId(row.id()),
                        row.day(),
                        row.meal(),
                        byId.get(row.recipe()),
                        row.servings(),
                        row.needsShopping()))
            .toList());
  }

  private void write(WeeklyPlan plan) {
    jdbc.sql(
            "DELETE FROM weekly_plans WHERE household_id = :household AND week_start = :start"
                + " AND id <> :id")
        .param("household", plan.household().value())
        .param("start", plan.weekStart())
        .param("id", plan.id().value())
        .update();
    jdbc.sql(
            """
            INSERT INTO weekly_plans (id, household_id, week_start) VALUES (:id, :household, :start)
            ON CONFLICT (id) DO NOTHING
            """)
        .param("id", plan.id().value())
        .param("household", plan.household().value())
        .param("start", plan.weekStart())
        .update();
    jdbc.sql("DELETE FROM plan_entries WHERE plan_id = :id")
        .param("id", plan.id().value())
        .update();
    for (PlanEntry entry : plan.entries()) {
      jdbc.sql(
              """
              INSERT INTO plan_entries (id, plan_id, day, meal, recipe_id, servings, needs_shopping)
              VALUES (:id, :plan, :day, :meal, :recipe, :servings, :shopping)
              """)
          .param("id", entry.id().value())
          .param("plan", plan.id().value())
          .param("day", entry.day())
          .param("meal", entry.meal().name())
          .param("recipe", entry.recipe().id().value())
          .param("servings", entry.servings())
          .param("shopping", entry.needsShopping())
          .update();
    }
  }

  private record EntryRow(
      UUID id, int day, Meal meal, UUID recipe, int servings, boolean needsShopping) {}
}
