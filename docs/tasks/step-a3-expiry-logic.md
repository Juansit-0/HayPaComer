# Step A3: Expiry with logic, estimates, and limits

Commit and pull request title: `feat(inventory): expiry with logic, estimates, and limits`

Owner: Jenifer Urbano (`Jenifrutica`). Third backend step of plan v2.

## Goal

An expiry date must make sense. Milk or meat cannot last a month in the fridge, and a person who does not know when the chicken expires still gets a useful date. HayPaComer estimates what is unknown, rejects what is impossible, and says where every date came from.

## Scope

- Flyway `V23__expiry_logic`:
  - `food_catalog` gets `fridge_days`, `door_days`, `freezer_days`, and `opened_days`, seeded with realistic values (chicken and ground beef 2 days in the fridge and 120 to 270 in the freezer, milk 7 closed and 4 opened, yogurt 10, dry goods 365);
  - `food_items` gets `expiry_source` (USER, LABEL, AI_SUGGESTED, ESTIMATED; existing dates become USER) and `opened_on`;
  - setting `food.expiry-margin-days` (3 by default, 0 to 30, per household);
  - Spanish templates for the new messages.
- Domain `expiry`:
  - `ShelfLife` gives the days by zone (shelf and drawer use the fridge days, door, freezer) and once opened (never longer than closed; the freezer ignores opening). Foods without seeded days fall back to `shelfDays`;
  - `ExpiryRules` estimates a date (`ESTIMATED`, confidence 0.6), rejects a date in the past or beyond the shelf life plus the margin with `ImpossibleExpiryException` and the reason, and brings a date closer after opening;
  - `ExpirySource` with its confidence (USER 1.0, LABEL 0.9, AI_SUGGESTED 0.7, ESTIMATED 0.6);
  - `FoodItem` keeps its expiry source and opening date (also in mementos and snapshots).
- Application:
  - port `ShelfLifeCatalog` and `PolicySource.expiryRules(household)`;
  - `ExpiryDesk` resolves the date for a stock command: estimate when missing, check when typed. The old constructors keep `ExpiryDesk.LENIENT`, so existing tests and callers behave as before;
  - `StockFoodCommand` takes `opened` and `expirySource` (for label photos in step A4) and uses the zone of the target tray;
  - `EstimateExpiry` previews a date; `MarkFoodOpened` opens an item, brings its date closer, and audits `OPEN_FOOD`.
- Web:
  - `POST /households/{h}/items` accepts `opened` and `expirySource`; an impossible date answers 422 "Expiry date not possible" with the reason;
  - inventory items include `expirySource`, `expiryConfidence`, and `openedOn`;
  - `GET /households/{h}/expiry-estimate?food=&zone=&opened=` and `POST /households/{h}/items/{id}/open`.

## Not in this step

Moving food to the freezer keeps its date; recalculating on moves comes with the add food flow in B4.

## Tests (definition of done)

- `ExpiryRulesTest`: milk 40 days and beef a month rejected with the reason, door and opened limits, the margin, the freezer, past dates, estimates by zone, opening only brings the date closer, fallback shelf life, item source and opening.
- `ExpiryInventoryTest`: estimates by tray zone and opening, rejection leaves the fridge empty, LABEL is kept, the lenient desk, previews for members only, opening with audit.
- `PostgresFridgeRepositoryTest`: source and opening round trip; `PostgresShelfLifeCatalog` with and without seeded days.
- `ExpiryIntegrationTest`: chicken without a date gets 2 days, milk 40 days is a Spanish 422, the door estimate, and opening milk and chicken.
