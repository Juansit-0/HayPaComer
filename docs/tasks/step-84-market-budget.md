# Step 84: Market agent with budget

Commit and pull request title: `feat(agent): market agent with budget`

## Goal

Shopping should respect the household's money. With a monthly budget, the market list shows what fits, puts the weekly plan's needs first, says what does not fit, and offers a cheaper allowed substitute when one exists. The market specialist reads the same numbers before it proposes anything.

## Scope

- Flyway `V19`: `market_budgets` (one monthly amount per household).
- Domain `market`: `BudgetPlanner` builds a `BudgetPlan` from the market list, the monthly amount, the start of the month, prices per kilogram, and substitution rules:
  - spent: items checked this month;
  - left: monthly minus spent;
  - priority: PLAN and RECIPE first, then AGENT_CONFIRMED, then MANUAL, oldest first;
  - `BudgetLine` per pending item (estimated cost, fits or not), with a `CheaperOption` when an allowed substitute (within its replaced limit) costs less;
  - unpriced items are listed apart.
- Application:
  - port `MarketBudgetRepository` with `PostgresMarketBudgetRepository`;
  - `SetMarketBudget` (MANAGE_MARKET_LIST, positive and at most 10,000,000,000);
  - `ViewMarketBudget` (any member, month in the household timezone, `MarketBudgetNotSetException` as 404);
  - `BudgetReport`.
- Agent: `review_budget` read tool; the MARKET specialist has it and reads it offline after the market list.
- Web:
  - `GET` and `PUT /households/{h}/market-budget`;
  - the Market screen shows money left, the cost of what fits, the monthly budget, what does not fit with cheaper options, and a form to set or update the budget.

## Tests (definition of done)

- `BudgetPlannerTest`: plan needs first, spent this month only, cheaper substitute, unpriced items, an overspent month, negative budgets.
- `MarketBudgetTest` (month start in the timezone, permissions, limits), `KitchenToolsTest` (`review_budget`), `PostgresOwnershipAndMovementsTest` (budgets), `MigrationsTest` (19 migrations), `MarketFlowIntegrationTest` (404 before a budget, PUT and plan lines, 400 for zero).
- Checked in the browser: setting a budget shows money left and a cheaper lentil option for black beans that do not fit.
