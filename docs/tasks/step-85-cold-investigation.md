# Step 85: Cold incident investigation

Commit and pull request title: `feat(agent): cold incident investigation`

## Goal

When a fridge gets warm, people need to know what happened and what to do with the food. The investigation rebuilds the timeline from measured door and temperature events, names the likely cause, and gives a verdict for every food by a fixed safety rule; the cold specialist explains it, but never decides it.

## Scope

- Domain `coldchain.investigation`:
  - `ColdInvestigator` turns temperature readings into `ColdEpisode`s above 5 C (start, end or still warm, peak, minutes above the limit counted between readings and up to now when ongoing);
  - door intervals from 30 minutes before each episode, and the likely cause `LikelyCause`:
    - DOOR_LEFT_OPEN when the door was open 40 s or more;
    - READINGS_MISSING when readings were more than 15 minutes apart;
    - otherwise COOLING_OR_POWER;
  - food verdicts (`FoodAssessment`, `FoodVerdict`) by the 2 hour rule:
    - perishable food above 5 C for 2 hours or more in total is DISCARD;
    - 30 minutes or more is USE_TODAY;
    - otherwise KEEP;
    - non-perishable food is always KEEP.
- Application:
  - port `SensorHistory` (implemented by `PostgresSensorEventLog`, using the existing fridge and time index);
  - `InvestigateColdIncidents`: members only, one or every fridge, 1 minute to 7 days back;
  - another member's private food appears as "Private food" without grams, but keeps its safety verdict with "tell its owner".
- Agent: `investigate_cold` read tool; the COLD specialist reads it offline between the cold chain and the expiries.
- Web: `GET /households/{h}/cold-investigation?fridgeId=&hours=24`.

## Tests (definition of done)

- `ColdInvestigatorTest`: door left open with use-today perishables, ongoing warmth with the door shut (discard), missing readings, a cold fridge, brief warmth, bad windows.
- `InvestigateColdIncidentsTest` (privacy, fridge filter, limits), `KitchenToolsTest` (`investigate_cold`), `PostgresSensorEventLogTest` (history query).
- `SensorIntakeIntegrationTest`: a real door plus cold chain break batch is investigated as DOOR_LEFT_OPEN, peak 11 C, still warm; 200 hours answers 400.
