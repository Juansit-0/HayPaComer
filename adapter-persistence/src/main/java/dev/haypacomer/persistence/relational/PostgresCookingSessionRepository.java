package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.port.CookingSessionRepository;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.session.Abandoned;
import dev.haypacomer.domain.session.Cooking;
import dev.haypacomer.domain.session.CookingSession;
import dev.haypacomer.domain.session.CookingSessionId;
import dev.haypacomer.domain.session.Finished;
import dev.haypacomer.domain.session.Paused;
import dev.haypacomer.domain.session.Preparing;
import dev.haypacomer.domain.session.SessionPhase;
import dev.haypacomer.domain.session.SessionState;
import dev.haypacomer.domain.session.StepCompletion;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
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
public class PostgresCookingSessionRepository implements CookingSessionRepository {

  private static final String SELECT =
      "SELECT id, household_id, started_by, recipe::text AS recipe, state, current_step,"
          + " started_at, state_since, updated_at FROM cooking_sessions";

  private final JdbcClient jdbc;
  private final TransactionTemplate transaction;

  public PostgresCookingSessionRepository(DataSource dataSource) {
    this.jdbc = JdbcClient.create(dataSource);
    this.transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
  }

  @Override
  public void save(CookingSession session) {
    transaction.executeWithoutResult(status -> write(session));
  }

  @Override
  public Optional<CookingSession> find(HouseholdId household, CookingSessionId id) {
    return load(
        " WHERE household_id = :household AND id = :id",
        Map.of("household", household.value(), "id", id.value()));
  }

  @Override
  public Optional<CookingSession> active(HouseholdId household) {
    return load(
        " WHERE household_id = :household AND state IN ('PREPARING', 'COOKING', 'PAUSED')",
        Map.of("household", household.value()));
  }

  private Optional<CookingSession> load(String where, Map<String, ?> params) {
    return jdbc.sql(SELECT + where)
        .params(params)
        .query(
            (row, rowNumber) ->
                new SessionRow(
                    row.getObject("id", UUID.class),
                    row.getObject("household_id", UUID.class),
                    row.getObject("started_by", UUID.class),
                    row.getString("recipe"),
                    SessionPhase.valueOf(row.getString("state")),
                    row.getInt("current_step"),
                    Timestamps.read(row, "started_at"),
                    Optional.ofNullable(row.getObject("state_since", OffsetDateTime.class))
                        .map(OffsetDateTime::toInstant)
                        .orElse(null),
                    Timestamps.read(row, "updated_at")))
        .optional()
        .map(this::toSession);
  }

  private CookingSession toSession(SessionRow row) {
    RecipePayload recipe = RecipePayload.read(row.recipe());
    Map<String, FoodMetadata> foods = new HashMap<>();
    FoodRows.load(jdbc, "WHERE name_key IN (:keys)", Map.of("keys", recipe.foodKeys()))
        .values()
        .forEach(food -> foods.put(food.key(), food));
    List<StepCompletion> completions =
        jdbc.sql("SELECT position, at FROM session_step_logs WHERE session_id = :id ORDER BY id")
            .param("id", row.id())
            .query(
                (log, rowNumber) ->
                    new StepCompletion(log.getInt("position"), Timestamps.read(log, "at")))
            .list();
    return CookingSession.restore(
        new CookingSessionId(row.id()),
        new HouseholdId(row.household()),
        recipe.toRecipe(foods),
        new UserId(row.startedBy()),
        row.startedAt(),
        state(row.phase(), row.currentStep(), row.stateSince()),
        row.updatedAt(),
        completions);
  }

  private static SessionState state(SessionPhase phase, int step, Instant since) {
    return switch (phase) {
      case PREPARING -> new Preparing();
      case COOKING -> new Cooking(step);
      case PAUSED -> new Paused(step, since);
      case FINISHED -> new Finished(step, since);
      case ABANDONED -> new Abandoned(step, since);
    };
  }

  private static Instant since(SessionState state) {
    return switch (state) {
      case Paused paused -> paused.since();
      case Finished finished -> finished.at();
      case Abandoned abandoned -> abandoned.at();
      case Preparing preparing -> null;
      case Cooking cooking -> null;
    };
  }

  private void write(CookingSession session) {
    Instant since = since(session.state());
    jdbc.sql(
            """
            INSERT INTO cooking_sessions (id, household_id, started_by, recipe, state,
                                          current_step, servings, started_at, state_since,
                                          updated_at)
            VALUES (:id, :household, :startedBy, :recipe::jsonb, :state, :step, :servings,
                    :startedAt, :since, :updatedAt)
            ON CONFLICT (id) DO UPDATE SET
                state = EXCLUDED.state,
                current_step = EXCLUDED.current_step,
                state_since = EXCLUDED.state_since,
                updated_at = EXCLUDED.updated_at
            """)
        .param("id", session.id().value())
        .param("household", session.household().value())
        .param("startedBy", session.startedBy().value())
        .param("recipe", RecipePayload.write(session.recipe()))
        .param("state", session.phase().name())
        .param("step", session.currentStep())
        .param("servings", session.recipe().servings())
        .param("startedAt", Timestamps.toDatabase(session.startedAt()))
        .param(
            "since",
            since == null ? null : Timestamps.toDatabase(since),
            Types.TIMESTAMP_WITH_TIMEZONE)
        .param("updatedAt", Timestamps.toDatabase(session.updatedAt()))
        .update();
    for (StepCompletion completion : session.completions()) {
      jdbc.sql(
              """
              INSERT INTO session_step_logs (session_id, position, at)
              VALUES (:session, :position, :at)
              ON CONFLICT (session_id, position) DO NOTHING
              """)
          .param("session", session.id().value())
          .param("position", completion.position())
          .param("at", Timestamps.toDatabase(completion.at()))
          .update();
    }
  }

  private record SessionRow(
      UUID id,
      UUID household,
      UUID startedBy,
      String recipe,
      SessionPhase phase,
      int currentStep,
      Instant startedAt,
      Instant stateSince,
      Instant updatedAt) {}
}
