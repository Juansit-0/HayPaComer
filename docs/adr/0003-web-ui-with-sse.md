# ADR 0003: Web UI with SSE instead of JavaFX

- Status: accepted
- Date: 2026-09

## Context

The original proposal offered JavaFX for the desktop demo or a web UI served by the backend. The live panel requires real-time updates, and the demo benefits from opening the UI on a phone or a second screen.

## Decision

Serve a static web UI from Spring with server-sent events for real time. JavaFX is discarded.

## Consequences

- Real-time panel without extra infrastructure; the ESP32 events and UI share one backend.
- Any browser becomes a client; no desktop packaging or JVM on the client.
- The UI depends on browser capabilities such as EventSource and Web Speech.
