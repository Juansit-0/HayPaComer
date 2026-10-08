# Step 62: clonable plan and recipe templates (prototype)

Commit and pull request title: `feat(planning): clonable plan and recipe templates (prototype)`

Depends on step 61 (stored recipes and weekly plans).

## Goal

A household can start from a good week or a favourite recipe instead of from zero: copy last week's plan to next week, or copy a template recipe and adjust it, without the copy and the original affecting each other.

## Pattern: Prototype

The object knows how to copy itself. Add a copy operation to the domain objects instead of rebuilding them field by field in the use cases:

- `Recipe.cloneAs(newId, household)` or a `RecipeTemplate` that produces new recipes; the copy remembers where it came from (`cloned_from`, already in the ER diagram).
- `WeeklyPlan.cloneFor(newWeekStart)` copies every entry with new ids and the new dates.
- Deep copy: changing an entry of the copy must not change the original (write a test for this).

## Scope

- Domain: the clone operations above and `is_template` / `source = TEMPLATE` for recipes.
- Application: `CloneWeeklyPlan`, `CloneRecipe`, `ListRecipeTemplates`.
- Persistence: migration for the missing columns if step 61 did not add them (`cloned_from`, `is_template`), and a few seeded global templates (`household_id` null) using foods from `V4__seed_food_catalog.sql`.
- Web: `POST /households/{h}/weekly-plans/{id}/clone` (body: week start), `GET /recipe-templates`, `POST /households/{h}/recipes/{id}/clone`.

## Tests (definition of done)

- Domain: clone has a new id, the same entries, `clonedFrom` set, and is independent from the original.
- Application: cloning a plan of another household returns not found; guests cannot clone.
- Persistence and web integration tests like in step 61.

## Docs to update

`docs/ROADMAP.md`, `AGENTS.md`, `docs/api.md`, `docs/database.md`, and the Prototype row in `docs/patterns.md` (say where it lives and the test that proves it).
