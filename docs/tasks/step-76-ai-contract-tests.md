# Step 76: AI response contract, weekly plan, and offline fallback tests

Commit and pull request title: `test(ai): response contract, weekly plan, and offline fallback`

## Goal

Close phase F6 by proving the AI layer behaves under real conditions: providers that wrap JSON in code fences, models that invent amounts, quota outages, models that reach for tools they should not have, and a weekly plan that must stay predictable.

## Scope

- `AiAgentScenarioTest` (agent): the supervisor with `LlmPlannerFactory` over a scripted model:
  - a grounded answer keeps measured grams;
  - an invented amount falls back to offline rules (audited FALLBACK);
  - a provider outage still answers from measured data;
  - a cold specialist cannot call `add_to_market` (REJECTED);
  - a model write waits for a person and then runs once.
- `ResponseContractFixturesTest` (adapter-ai): fenced JSON from real providers is accepted for intents and photo recipes; prose around JSON is still rejected.
- `WeeklyPlanBoundariesTest` (domain):
  - the same fridge always gives the same week, whatever the recipe order;
  - rescue first even without enough stock;
  - rescue points stop once the at-risk food is used;
  - an empty fridge still plans every meal and marks shopping.
- Fix found by these tests: `JsonContract` and `LlmPlanner` accept answers wrapped in a Markdown code fence (```json ... ```), which some providers return even in JSON mode, while still rejecting prose.
