# Messaging Framework

## 01 Core message

HayPaComer helps shared and family households decide tonight's dinner from what is actually in the fridge by weighing stock, tracking expiry and cold chain, and deciding with rules that keep working offline.

## 02 Value proposition

HayPaComer turns a normal fridge into a measured one: an affordable scale and sensors keep stock in grams, expiry dates, and cold-chain state, then answer the nightly question with evidence. It is built for homes where several people share the same fridge, so ownership and permissions are respected as carefully as the numbers. Unlike camera apps and inventory lists, it measures instead of guessing, and its rules keep deciding even when the internet or the AI does not.

## 03 Tagline options

- **Weigh it. Know it. Cook it.** - Functional. Mirrors the product loop in three verbs and makes measurement the hero. **Recommended.**
- **Dinner starts with what is actually home.** - Emotional. Turns the nightly question into a promise and fits the brand voice.
- **Measured food, honest answers.** - Functional. States the category contrast (measurement versus guessing) in four words.
- **Know your fridge in grams.** - Functional. Concrete and impossible to confuse with camera-app claims.
- **Cook what you have. Prove it.** - Aspirational. Pairs warmth with the verifiable-evidence stance.

Evaluation of the strategy deck's existing suggestion: "Weigh it. Know it. Cook it." remains the strongest because it is the only one that describes action, not aspiration, and it survives translation into notifications and UI copy.

## 04 Messaging hierarchy

**Level 1 - Brand headline**

> Your fridge, weighed and honest.

**Level 2 - Supporting statement**

HayPaComer keeps your real stock in grams, warns what expires first, and answers what you can cook tonight - offline and without inventing safety.

**Level 3 - Key messages**

- **Measured, not guessed**
  Stock, portions, and substitutions come from a scale, not from a camera or a model. A recipe needing 200 g against 80 g measured becomes a precise decision: reduce or substitute.
  *Proof:* Fridge mode records 842 g to 650 g as "192 g used", container weight already tared.

- **Fair in a shared fridge**
  Food has owners and permissions. The system never suggests someone else's food without authorization, and the household list stays in sync without duplicates.
  *Proof:* ownership and permission checks run before any suggestion; the market list groups missing items per product.

- **Rules decide safety, AI helps elsewhere**
  Cold chain and risk come from explicit time and temperature rules, never from a language model. AI removes friction in registering, explaining, and suggesting.
  *Proof:* an out-of-range event produces "under review" from the cold rule; the AI can fail without stopping alerts.

- **Works when everything else fails**
  Inventory, alerts, scale flows, and rule-based suggestions keep working without WiFi or AI, through degraded mode and a circuit breaker.
  *Proof:* demo scenario 8: the AI API goes down and the whole system keeps cooking.

**Level 4 - Proof bank**

- 842 g to 650 g on the tray equals a recorded 192 g consumption, tare included.
- 200 g required versus 80 g measured triggers reduce or weighed-substitute flows.
- 130 g of tuna is validated against profile allergies before it is accepted.
- Door open for 40 seconds triggers buzzer, Telegram, and a live SSE state change.
- Cold chain flags "under review" with time and temperature, never a sanitary guarantee.
- The market list is deduplicated by product across the whole household.
- All 23 GoF patterns run in production flows with their own tests and documented roles.
- CI keeps the build green with coverage above 80% in domain and application.

## 05 Audience-specific messaging

**For shared households (roommates, students):**
- What they care most about: fairness, speed, and not wasting money they pooled.
- How to lead: ownership first, weight second: "Ana's cheese stays Ana's. The rice is shared. Dinner takes 15 minutes."
- Proof points that land hardest: permission checks, the measured tray discount, the deduplicated shared list.
- Avoid: family framing, meal-planning language, anything that sounds like homework.

**For families:**
- What they care most about: safety, money, and calm weeknights.
- How to lead: evidence and protection: "Know what expires first, what is safe to keep, and what dinner costs nothing extra."
- Proof points that land hardest: cold-chain "under review", rescue-first suggestions, kg and money saved per month.
- Avoid: alarmism, health scoring, guilt about leftovers.

**For evaluators and the open-source audience (secondary):**
- What they care about: engineering rigor and honesty of claims.
- How to lead: verifiable craft: pattern coverage, boundary tests, the agent that never writes to the database.
- Proof points that land hardest: architecture docs, ArchUnit boundaries, demo acceptance criteria.
- Avoid: marketing superlatives; show the tests and the trace.

## 06 Messaging by channel

| Channel | Headline approach | Tone | Length |
|---|---|---|---|
| Website hero | Lead with the measured promise plus one number | Calm, concrete | Short |
| Social bio | One-line positioning with weight in it | Direct, warm | Very short |
| Email subjects | The fact and the action | Plain, no urgency | Short |
| Demo and pitch deck | Problem in plain kitchen language, proof in numbers | Confident, honest | Medium |
| Paid ads | The nightly question answered in grams | Warm, practical | Very short |

## 07 Things not to say

- Any claim without a demonstrable proof point (no "AI nutritionist", no "detects freshness").
- Category cliches: "reduce food waste" as a headline, "smart kitchen platform", "seamless experience".
- Competitor-adjacent language: camera scanning, fridge cameras, wellness scores, smart-home ecosystem talk.
- Health or sanitary guarantees: the product flags "under review"; it never certifies food as safe.
- Guilt or urgency: wasted money is a metric here, never an accusation.
- Subscription framing for basics: weighing, alerts, and rules are always included.
