# Step 86: Anti-waste coach and weekly digest

Commit and pull request title: `feat(agent): anti-waste coach and weekly digest`

## Goal

Help the household waste less week after week. The coach spots foods that keep being thrown away and suggests how much less to buy; every Monday morning a short digest compares the week with the previous one and thanks whoever rescued the most food.

## Scope

- Domain `analytics`:
  - `WastePatterns.find`: foods discarded at least twice, the 3 biggest by grams, with a "buy about N% less" (share thrown away rounded to tens, 10 to 90 percent) in `WasteTip`;
  - `WeeklyDigest`: rescued grams and money, grams thrown away with the waste rate and its change in points against the previous week, the top rescuer by name, and the tips (or a calm line when nothing was wasted twice).
- Application:
  - `BuildWeeklyDigest`: the last 7 full days against the 7 before, in the household timezone;
  - `FindWastePatterns`: 1 to 90 days.
- Agent:
  - `waste_patterns` read tool, which the COACH specialist reads offline after the expiries;
  - `ScheduledBriefings` publishes "Your week in the kitchen" on Mondays at 8:00 local time, once per week (`BriefingLog`), through `Briefer.publish`. It is deterministic text, no model involved.
- Web: `GET /households/{h}/analytics/weekly-digest`.

## Tests (definition of done)

- `WeeklyCoachTest`: tips from repeated discards and the buy-less rounding, digest text with better, same, and worse weeks, and the top rescuer.
- `AnalyticsUseCasesTest` (digest week boundaries and patterns), `ProactiveBriefingsTest` (Monday digest once, not on Tuesday), `KitchenToolsTest` (`waste_patterns`), `AnalyticsIntegrationTest` (digest endpoint).
