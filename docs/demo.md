# Demo script

A 12 minute walk through what HayPaComer does, following the acceptance criteria in `PLAN.md` section 10. Two people run it: **Juan** (owner, on a laptop) and **Ana** (member, on a phone). Every scene has a plan B that does not need hardware, so the demo never depends on a cable.

## Before the audience arrives (15 minutes)

1. Start the databases and the app:

   ```bash
   cp .env.example .env
   docker compose up -d
   mvn -q package -DskipTests
   MAIL_LOG_LINKS=true java -jar web/target/web-1.0.0.jar > demo.log 2>&1 &
   ```

   Set a real `JWT_SECRET` in `.env`. Leave `AI_PROVIDER=offline` unless a provider key is set, because scene 8 shows the offline path anyway. Optionally set `TELEGRAM_BOT_TOKEN`.

2. Prepare the household:

   ```bash
   python3 scripts/demo/seed.py --log demo.log
   ```

   - It creates Juan and Ana with generated passwords, the household "Apartment 402" (COP, Bogota time), a fridge with milk (892 g gross, 50 g jar), Juan's private yogurt, 80 g of chicken and 360 g of tomato expiring tomorrow, tuna, rice, and eggs.
   - It also creates the door sensor and the counter scale (simulated devices, with the milk on the scale), the "Rice with chicken" recipe, a 200,000 COP market budget, Ana's invitation (accepted from the log), and Ana's fish allergy.
   - Passwords and device keys are saved in `.demo-credentials.json`, which Git ignores.

3. Sign in as Juan on the laptop (`http://localhost:8080`) and as Ana on the phone, on the same network. Open Now on both. The interface follows the browser language; switch Español or English in the header (or in Settings), and the choice is saved for each person. Keep Swagger UI (`/docs`) open in a second tab, authorized with Juan's access token, for the steps the web screens do not cover yet (grants, recipe evaluation, starting a cooking session, missing items).

4. Rehearse the sensor commands once against a second household or after a reset. The cold chain break must run **before** any newer temperature, because older readings are dropped as stale.

## Scenes

| # | Minute | What the audience sees | Who | How |
|---|---|---|---|---|
| 0 | 0:00 | Ana cannot use Juan's private yogurt; Juan shares it and she can | Ana, Juan | Fridge tab: Ana sees "Private food" without grams and cannot use it. Juan grants Ana in Swagger (`POST /households/{h}/items/{id}/grants`). Ana marks 50 g as used |
| 1 | 1:30 | Door open 40 s: buzzer command, Telegram, live panel, a briefing on what to check | Laptop | Open the real door, or `python3 scripts/demo/sensors.py door-open --seconds 45`; wait about 5 s for the alert check. Now shows the live alert, Ana's phone shows the unread alert, and a "What to check after: Door left open" briefing follows |
| 2 | 3:00 | Taking milk from the fridge on the scale: 842 g becomes 650 g; adding chicken without a date shows "≈ expires in 2 days (estimated)" | Laptop | Lift the jar, or `python3 scripts/demo/sensors.py weigh 700`. Fridge tab shows Milk 650 g; Activity shows a SCALE consumption of 192 g |
| 3 | 4:00 | "Organize dinner and what to buy": the supervisor splits the work and shows its trace | Laptop | Chef tab, ask "Organize dinner and what to buy so nothing goes to waste". Open "What this is based on". Show `GET /agent/runs/{id}/trace` in `/docs` for the tool calls and grams. Anything that writes waits under "Waiting for your yes" |
| 4 | 5:30 | Guided cooking with voice | Phone | Start "Rice with chicken" for two in Swagger (`POST /households/{h}/cooking-sessions`). On the phone, Chef tab, tick Hands-free, say "siguiente", "pausa", "sigue", "repite". The step card follows the voice |
| 5 | 7:00 | The recipe asks for 200 g of chicken, there are 80 g: reduce or substitute | Laptop | Evaluate the recipe in Swagger (`POST /households/{h}/recipes/evaluate`; FLEXIBLE: one serving; RESCUE: tuna for the missing 120 g). While weighing, the scale copilot says "Add 90 g more Rice (40%)" and offers to rescale the rest if you pour too much |
| 6 | 8:00 | Tuna is weighed: Java checks Ana's allergy, discounts stock, and updates the market list | Laptop | Evaluate RESCUE with Ana as a diner: tuna is refused because of her fish allergy. Weigh 130 g of tuna and use it; add the missing chicken with `POST /households/{h}/recipes/missing-to-market`; the Market tab's budget panel shows what fits |
| 7 | 9:00 | A recipe photo becomes a verified draft | Laptop | With an AI provider: `POST /recipes/from-photo` with a cookbook photo; each ingredient shows the catalog food and grams or why it could not be verified; nothing is saved. Offline it answers 503 with a clear message, which is a good bridge to scene 8 |
| 8 | 10:00 | The AI goes down and nothing breaks | Laptop | Remove the key or point to a wrong base URL and restart; ask the Chef again. Answers come from the offline rules, `/api/v1/status` shows the provider resting, and suggestions, alerts, and stock keep working |
| 9 | 11:00 | What the fridge saved | Laptop | Numbers tab: grams rescued, money saved, waste rate, the ranking with Ana and Juan, and the report download. Close with the weekly digest (`/analytics/weekly-digest`) |

## What to say in each scene

- **Scene 0:** visibility and grants are per member. The private food Proxy hides grams and dates; the Chain of Responsibility checks every write.
- **Scene 1:**
  - Sensor events pass a validation chain (range, clock skew, stability, duplicates, staleness) before the fridge monitor (Bridge) raises one alert per episode.
  - Notifications fan out through Observer and Strategy channels.
  - The cold specialist explains, but the 40 s threshold is a rule.
- **Scene 2:** grams are measured, never estimated. The scale reading minus the jar tare becomes a Command with the sensor event id, so a resend never discounts twice. Concurrent uses of the same food are serialized per household.
- **Scene 3:**
  - The supervisor routes to at most two specialists, each with its own tool allowlist.
  - Tools pass schema and permission guardrails.
  - Writes become pending confirmations, and approval re-checks permissions before the real use case runs once.
  - Answers quoting grams no tool measured are refused.
- **Scene 4:** cooking sessions are a State machine; timers, the scale, and the session talk through a Mediator. Voice runs in the browser; only text reaches the server.
- **Scene 5:** evaluation is a Strategy (strict, flexible, rescue); quantities like "0,15 kg" go through the Interpreter.
- **Scene 6:** substitutions respect proportion, the replaced limit, and every diner's allergies; the budget planner puts plan needs first and suggests cheaper allowed substitutes.
- **Scene 7:** the model only reads the photo; Java verifies each food against the catalog and each quantity in grams.
- **Scene 8:** one circuit breaker per provider covers suggestions, chat, and photos; the Null Object chat model defers to the rules; reference data keeps a last saved copy, but stock is never served from a copy.
- **Scene 9:** analytics come from measured movements only; reports are a Template Method over a Visitor.

## If something goes wrong

| Problem | Plan B |
|---|---|
| The ESP32 is not connected | Use `scripts/demo/sensors.py` (door-open, door-close, temperature, cold-break, weigh); the devices are simulators |
| The door alert does not appear | Wait 5 s (the alert check runs every 5 s) and make sure the door event is at least 40 s old |
| The cold chain break shows only one reading | It ran after a newer temperature; run it first on a fresh seed |
| Voice is not available | Firefox and some phones do not listen; type the same commands in the Chef box, or use the step card buttons |
| Telegram is silent | The inbox and live panel still show every alert; Telegram needs `TELEGRAM_BOT_TOKEN` and the chat id in Ana's preferences |
| Ana's invitation was not accepted | Run `seed.py` with `--log` pointing at a log written with `MAIL_LOG_LINKS=true`, or open the link from the log as Ana |
| The database restarts | Writes answer 503 with `Retry-After` and the saved mode notice explains what is still available; reload when it is back |

## Reset

```bash
docker compose down -v && docker compose up -d
rm -f .demo-credentials.json
```

Then start the app again and rerun `scripts/demo/seed.py`.

## Automated rehearsal

`DemoFlowIntegrationTest` runs scenes 0 to 9 against real PostgreSQL and Redis on every build, plus a 500-event noise batch and 20 simultaneous uses of one food, so the script is checked even when nobody rehearses it.
