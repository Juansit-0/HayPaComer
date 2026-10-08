# Step 63: plan delta into the market list

Commit and pull request title: `feat(planning): plan delta into the market list`

Depends on step 61 (weekly plans).

## Goal

From the current weekly plan, compute what the household must buy: the total grams every planned recipe needs, minus what is usable in the fridge, minus what is already pending on the market list, and add only that difference to the list.

## What already exists

- `application/.../market/AddMissingToMarketList.java` (step 54) does this for one recipe; read it and its test first.
- `domain/.../market/MarketList.java`: `pendingGrams(food)` and `topUp(food, needed, source, user, at)` already add only the missing gap, so repeating the request never doubles the list.
- `MarketSource.PLAN` already exists for items that come from the plan.

## Scope

- Domain or application: sum the requirements of every plan entry (scaled to its servings) per food, then compare with availability once for the whole week, not recipe by recipe (two recipes with 100 g of rice each need 200 g in total).
- Application: `AddPlanDeltaToMarketList` (permission `MANAGE_MARKET_LIST`) returning food, needed, available, added, pending.
- Web: `POST /households/{h}/market-list/from-plan`.

## Tests (definition of done)

- Two entries that share a food add up before comparing with the fridge.
- Optional requirements are not bought.
- Running it twice adds nothing the second time.
- Food that is expired or private to another member does not count as available.
- Integration test through the endpoint.

## Docs to update

`docs/ROADMAP.md`, `AGENTS.md`, `docs/api.md` (`/market-list/from-plan` row).
