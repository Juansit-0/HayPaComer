# Step 83: Reactive copilot with live scale

Commit and pull request title: `feat(agent): reactive copilot with live scale`

## Goal

While someone cooks with the scale, the kitchen reacts to every stable reading in plain words: how much more to add, when the step is on target, and what to do after pouring too much. The advice is computed in Java from measured grams, never guessed by a model.

## Scope

- Agent `copilot.ScaleCopilot` (a `WeightReadingHandler`):
  - reacts only to stable COOKING readings of a scale that has a weighing target;
  - SHORT: "Add 90 g more Rice (40%).";
  - ON_TARGET: "Rice is on target at 150 g. Confirm the step to go on.";
  - OVER: "30 g over on Rice (180 g of 150 g). Take 30 g out, or keep it and scale the rest by 1.20: Chicken breast 240 g instead of 200 g." The rest of the active session's recipe is scaled by measured over target (up to three foods), and the second part is omitted when no session uses that scale;
  - publishes a COPILOT live update only when the hint changes (status, a new tenth of progress, or a new over amount), so heartbeats do not flood the screen.
- Application:
  - `LiveUpdateKind.COPILOT`;
  - `WeightReadingHandlers` runs every handler for each reading and keeps the first outcome, so the fridge stock discount and the copilot both see each reading without changing `IngestSensorEvents`.
- Web:
  - `DeviceConfiguration` wires `WeightReadingHandlers(ApplyFridgeScaleReading, ScaleCopilot)`;
  - the live feed labels copilot hints "Scale", and the fridge twin ignores them.

## Tests (definition of done)

- `ScaleCopilotTest`: guidance through a pour with deduplication and unstable readings ignored, over-pour scaling of the rest of the recipe, silence outside guided cooking, all handlers run with the first outcome kept.
- `LiveStreamIntegrationTest`: a simulated scale in cooking mode streams `event:copilot` with "Add 90 g more Rice (40%)." over SSE.
