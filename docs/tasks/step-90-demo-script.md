# Step 90: Expanded demo script

Commit and pull request title: `docs(demo): expanded demo script`

## Goal

Anyone on the team can run the final demo in 12 minutes without improvising, with or without the ESP32 hardware, and recover from anything that fails in front of the audience.

## Scope

- `docs/demo.md`:
  - preparation;
  - ten scenes with minute marks, who does what, and what the audience should see, following `PLAN.md` section 10;
  - the design patterns and safety rules to explain in each scene;
  - a plan B table, a reset, and the automated rehearsal (`DemoFlowIntegrationTest`).
- `scripts/demo/seed.py` (Python 3, standard library only) prepares the demo household through the public API:
  - Juan and Ana with generated passwords;
  - the household, the fridge, and the demo food, including private yogurt and chicken expiring tomorrow;
  - simulated door and scale devices with the milk on the scale;
  - the "Rice with chicken" recipe, a market budget, Ana's invitation accepted from the app log, and her fish allergy.
- `scripts/demo/sensors.py` sends door, temperature, cold chain break, and scale events as the ESP32 would, and reports accepted, rejected, duplicate, and dropped events.
- Generated credentials and device keys live in `.demo-credentials.json`, which Git ignores.

## Checked

- Run against the real jar and Docker databases:
  - seed made Ana a MEMBER;
  - the door open 45 s produced `DOOR_LEFT_OPEN` and a briefing for Ana;
  - the scale took the milk to 650 g.
- The cold chain break showed that readings older than the latest temperature are dropped as stale, so the script says to send it first.
