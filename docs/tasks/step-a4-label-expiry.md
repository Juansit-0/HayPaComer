# Step A4: Expiry from a label photo and shelf life advice

Commit and pull request title: `feat(ai): expiry from a label photo and shelf-life advice`

Owner: Jenifer Urbano (`Jenifrutica`). Last backend step of plan v2.

## Goal

A person takes a photo of the package and HayPaComer proposes the expiry date. The AI only reads the label; Java decides whether the date makes sense. When the AI is down, or the date is missing, unsure, or impossible, the person still gets the usual shelf life with the reason.

## Scope

- Application:
  - `LabelReading` (product, printed date, confidence, source) and port `LabelPhotoReader`;
  - `ReadExpiryFromLabel` checks membership and the food, applies the AI rate limit, audits each call, and returns an `ExpiryProposal` that is never saved;
  - a readable date with confidence 0.6 or more passes through `ExpiryDesk` as LABEL (confidence up to 0.9);
  - otherwise the proposal is the ESTIMATED date from step A3 with the reason: no date, unsure, the rejection message of `ExpiryRules`, AI not available, or label not readable.
- `adapter-ai`:
  - `LlmLabelPhotoReader` (strict JSON contract: product up to 80 characters, ISO date or null, confidence 0 to 1; Colombian day, month, year labels);
  - `OfflineLabelPhotoReader` and `ResilientLabelPhotoReader` on the shared provider circuit.
- Agent: read tool `estimate_expiry` (food, place fridge, door, or freezer, opened) for the chef specialist, so the Chef answers "when does this expire?" with the rule.
- Web:
  - `POST /households/{h}/items/expiry-from-photo` (multipart `photo`, `food`, `zone`, `opened`) answers the proposal with a localized reason and `saved: false`;
  - Flyway `V25` adds the Spanish reasons.

## Tests (definition of done)

- `ReadExpiryFromLabelTest`: label date proposed, impossible, missing, unsure, and past dates fall back with their reason, outages and unreadable labels, audit, membership, unknown food, rate limit.
- `LlmLabelPhotoReaderTest`: contract, null date, non-ISO date, outage, offline reader, and the circuit opening.
- `EstimateExpiryToolTest`: fridge, freezer, opened door, and unknown places.
- `LabelExpiryIntegrationTest`: LABEL, impossible date estimated, Spanish reason when the AI is down, 401, and 422 for an unknown food.
