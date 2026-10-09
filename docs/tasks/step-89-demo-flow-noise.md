# Step 89: Full demo flow and extreme noise

Commit and pull request title: `test(integration): full demo flow and extreme noise`

## Goal

Prove the acceptance demo end to end over HTTP with real PostgreSQL and Redis, and prove that sensor floods and simultaneous users cannot corrupt the stock.

## Scope

- `DemoFlowIntegrationTest.theAcceptanceDemoRunsEndToEnd` follows `PLAN.md` section 10:
  - Ana joins by invitation and cannot use Juan's private yogurt until he grants it;
  - a door open for 45 s sounds the buzzer command and notifies Ana;
  - the scale takes milk from 842 g to 650 g;
  - the supervisor answers "organize dinner and what to buy" with two specialists and a visible trace;
  - the FLEXIBLE strategy reduces to one serving for 80 g of chicken, RESCUE offers tuna, and tuna disappears when a diner is allergic to fish;
  - a guided cooking session advances;
  - the photo reader answers 503 without an AI provider while status stays OK and the chef chat still answers;
  - analytics count the 80 g of chicken rescued and the money saved.
- `extremeSensorNoiseNeverCorruptsTheStock`:
  - one 500-event batch mixes valid temperatures, out-of-range spikes, scale jitter of a few grams, unstable garbage weights, door flapping, readings an hour in the future, and 20 repeated events;
  - every event is accounted for (accepted, rejected, duplicates, dropped), the resend is reported as duplicates, and malformed JSON answers 400;
  - the milk stays at 842 g with no consumption logged.
- `concurrentUseOfTheSameFoodNeverLosesGrams`: 20 parallel requests take 50 g each from 842 g; exactly 16 succeed, the rest answer 400 for lack of stock, the item ends at 42 g, and the activity log has 16 consumptions.

## Bug found and fixed

- Concurrent inventory commands lost updates: two transactions read the same fridge and the last write won. In the first run 20 parallel uses of 50 g all answered 200 and only 150 g left the fridge.
- `UnitOfWork.runFor(household, work)` was added (by default the same as `run`).
- `PostgresUnitOfWork` takes a transaction-scoped advisory lock per household (`pg_advisory_xact_lock(hashtextextended(household, 0))`) before the idempotency check.
- `ExecuteInventoryCommand`, `UndoLastChange`, and `RestoreSnapshot` use it, so commands of one household run one after another while other households are not blocked.
