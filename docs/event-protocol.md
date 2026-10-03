# Event protocol

## Envelope

```json
{
  "eventId": "7f9c2b1e-9d3a-4c5f-8e2b-1a6d4f0c9b77",
  "device": "fridge-01",
  "type": "WEIGHT",
  "mode": "COOK",
  "door": "CLOSED",
  "tempC": 5.2,
  "grams": 132,
  "stable": true,
  "ingredient": "onion",
  "at": "2026-09-04T18:40:00Z"
}
```

## Event types

| Type | Fields | Meaning |
|---|---|---|
| DOOR | door | OPEN or CLOSED with timestamp |
| TEMPERATURE | tempC | Internal temperature reading |
| WEIGHT | grams, stable, mode (`FRIDGE` default or `COOK`), ingredient | Scale reading after tare |

## Validation

- JSON schema per type; missing required fields reject the event, unknown fields are ignored.
- Ranges: tempC in [-30, 60]; grams in [0, 20000]; timestamps within accepted skew.
- Weight is accepted only when `stable` is true (about one second of stable reading after tare).
- Idempotency: `eventId` is unique; duplicates are ignored without side effects.
- Ordering: events are processed in arrival order per device; stale readings older than the last accepted reading are dropped.

## Decoding

- `Esp32EventAdapter` (module `adapter-sensors`) turns the envelope into domain events through one factory per type (`DOOR`, `TEMPERATURE`, `WEIGHT`, case-insensitive).
- The device and fridge always come from the authenticated device key; the `device` field is informational.
- A payload may be one envelope or an array of up to 500 envelopes (buffered replay after reconnecting), decoded in order.
- `Esp32Simulator` produces the same envelopes for demos: door left open, cold-chain break, and product removal (842 g to 650 g).

## REST intake

- `POST /api/v1/device/events` with `X-Device-Key` accepts one event or a batch and returns `202 Accepted` with the processing result.
- Validation failures return RFC 7807 `400`; duplicates return `200` with `duplicate: true`.
- Devices retry with backoff on `5xx`; the backend is safe to retry thanks to idempotency.

## Degraded behavior

- Without connectivity, the ESP32 buffers events locally and replays them on reconnect.
- Without AI, events are still validated, stored, and trigger rule-based alerts and notifications.
