# Step 61: 7-day weekly plan with rescue-first strategy

Commit and pull request title: `feat(planning): 7-day weekly plan with rescue-first strategy`

## Goal

A household asks for a week of meals and gets 7 days of lunch and dinner where the first days use the food that is about to expire, every dish is safe for the people eating, and nothing is planned that the fridge cannot support without being marked as "needs shopping".

## What already exists (read these first)

| Need | Reuse |
|---|---|
| Recipe model, scaling to servings | `domain/.../recipe/Recipe.java`, `RecipeRequirement`, `RecipeStep` |
| Evaluate a recipe against the real inventory | `application/.../cooking/EvaluateRecipe.java` (STRICT, FLEXIBLE, RESCUE) and `domain/.../cooking/*Strategy.java` |
| What expires first | `application/.../inventory/ViewInventory.java` (sorted with `RESCUE_ORDER`), `domain/.../inventory/StockedFood.java` (`rescuePriority()`, `AtRiskFood`) |
| Allergies, diets, avoided foods | `domain/.../member/DiningGroup.java`, `application/.../profile/ListFoodProfiles.java` |
| Access rules | `application/.../household/GetHousehold.java`, `Permission.COOK` and `Permission.VIEW_HOUSEHOLD` |
| Example of a full feature end to end | the cooking session (step 51): `domain/.../session`, `application/.../session`, `PostgresCookingSessionRepository`, `CookingSessionController`, `CookingSessionIntegrationTest` |

Recipes are not stored in the database yet (evaluation and sessions receive them inline). The plan needs stored recipes, so this step adds the minimum to keep them.

## Scope

### Domain (`dev.haypacomer.domain.planning`)

- `Meal` enum (`LUNCH`, `DINNER`; add `BREAKFAST` only if you also plan it).
- `PlanEntry` (day 1-7, meal, recipe, servings, a flag or verdict telling if it needs shopping).
- `WeeklyPlan` aggregate (id, household, week start date, entries; one entry per day and meal; change an entry).
- `PlanningStrategy` interface and `RescueFirstStrategy` implementation (Strategy pattern): orders candidate recipes by how much at-risk food they use, skips recipes the `DiningGroup` cannot share, avoids repeating a recipe on consecutive days, and fills the 14 slots.

### Application (`dev.haypacomer.application.planning`)

- Port `RecipeRepository` (save, find by id, list by household) and port `WeeklyPlanRepository` (save, current plan of a household).
- Use cases, one public method each: `SaveRecipe`, `ListRecipes`, `GenerateWeeklyPlan`, `ViewCurrentPlan`, `ChangePlanEntry`.
- `GenerateWeeklyPlan` builds availability like `EvaluateRecipe` does (usable and edible food only) and uses the household timezone for the week start.

### Persistence

- Flyway migration with `recipes`, `recipe_requirements`, `recipe_steps`, `weekly_plans`, `plan_entries` following `docs/database.md` section 4.
- `PostgresRecipeRepository` and `PostgresWeeklyPlanRepository` with `JdbcClient`, tested with Testcontainers like `PostgresCookingSessionRepositoryTest`.

### Web

- `POST /households/{h}/recipes`, `GET /households/{h}/recipes` (reuse `RecipeAssembler` and `RequirementRequest`/`StepRequest` from `web/.../cooking`).
- `POST /households/{h}/weekly-plans` (generate), `GET /households/{h}/weekly-plans/current`, `PATCH /households/{h}/plan-entries/{id}`.
- Beans in a new `web/.../planning/PlanningConfiguration.java`.

## Tests (definition of done)

- Domain: the strategy puts the recipe that uses the expiring chicken on day 1, never plans a recipe with an allergen of a diner, does not repeat a recipe on consecutive days, and marks entries that need shopping.
- Application: in-memory test like `EvaluateRecipeTest` with a household, a fridge, and stored recipes; guests can view, only members can generate or change.
- Persistence: round trip of a recipe and a plan.
- Web: integration test that creates recipes, generates a plan, reads it, and changes one entry; `SecurityIntegrationTest` must keep passing (it checks every new endpoint needs a token).
- `mvn verify` green, JaCoCo at least 80% in `domain` and `application`.

## Docs to update in the same pull request

`docs/ROADMAP.md` (check step 61), `AGENTS.md` (state and next action), `docs/api.md` (new endpoints), `docs/database.md` (tables and migration note), `docs/patterns.md` (Strategy row: weekly plan).
