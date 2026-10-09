# Step 77: Consumption, avoided waste, and money saved

Commit and pull request title: `feat(analytics): consumption, avoided waste, and money saved`

## Goal

Show a household what its fridge achieved: grams eaten, grams rescued before they expired, grams thrown away, and what that means in money. The numbers come only from measured inventory movements, never from estimates.

## Scope

- Flyway `V18`:
  - `inventory_movements` gains `food_key` and `expires_on`, filled for every new movement, so analytics survive items being removed;
  - index on `(household_id, type, at)`;
  - `food_catalog.reference_price_cop_per_kg` seeded for the 24 foods;
  - `food_prices` for household overrides.
- Domain `analytics`:
  - `MetricsCalculator` over movements: CONSUME counts as consumed;
  - it is rescued when the item expired within the at-risk window from that day (not expired yet);
  - DISCARD is waste;
  - totals, per food, per member (ranked by rescued grams), per day, money saved and wasted at the price per kg, and foods without a price;
  - `Tally`, `FoodTally`, `MemberTally`, `DayTally`, `HouseholdMetrics`.
- `InventoryMovement` carries `foodKey` and `expiresOn` (legacy rows without them still count as "unknown").
- Application:
  - ports `MovementHistory` and `FoodPriceRepository`;
  - `ViewHouseholdMetrics` (any member, household timezone and currency, 1-366 days);
  - `SetFoodPrice` (MANAGE_MARKET_LIST, catalog foods, positive price);
  - `ListFoodPrices`.
- Persistence:
  - `PostgresInventoryMovementLog` writes the new columns and reads a period;
  - `PostgresFoodPriceRepository` uses the reference prices for COP households, then the household overrides.
- Web: `AnalyticsController`:
  - `GET /households/{h}/analytics?from=&to=` (default last 30 days);
  - `GET` and `PUT /households/{h}/prices`.

## Tests (definition of done)

- `MetricsCalculatorTest`: rescued versus late consumption, waste, money, per food, member, and day, empty periods, legacy movements.
- `AnalyticsUseCasesTest`, `LiveInventoryTest` (movements carry their food), `PostgresOwnershipAndMovementsTest` (columns, period query, prices), `MigrationsTest` (18 migrations, 24 priced foods).
- `AnalyticsIntegrationTest`: 250 g of chicken rescued (5,500 COP), 500 g of rice thrown away (2,400 COP), a household price changes the money, unknown food 422, reversed period 400, stranger 404.
