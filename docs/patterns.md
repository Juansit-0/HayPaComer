# Design patterns

HayPaComer covers 23/23 GoF patterns. Honesty rule: each pattern is used in a real production flow, has a specific test, and a justified row here. This document is updated in the same PR that implements each pattern.

| Category | Pattern | Role in HayPaComer | First delivery |
|---|---|---|---|
| Creational | Singleton | `FridgeSession` coordinates one physical fridge with a single active state | F6 |
| Creational | Factory Method | Creates door, weight, and temperature events and food types | F3 |
| Creational | Abstract Factory | Real ESP32 hardware vs simulated hardware | F3 |
| Creational | Builder | Builds a suggestion, a weekly plan, or a cooking session step by step | F6 |
| Creational | Prototype | Clonable weekly-plan and recipe templates | F6 |
| Structural | Adapter | ESP32 JSON, OCR, and AI responses into the common Java model | F3 |
| Structural | Bridge | Measurement source (door, weight, temperature) x interpretation (stock, cold chain, usable quantity) | F3 |
| Structural | Composite | Fridge -> zone -> tray -> food as a navigable tree | F1 |
| Structural | Decorator | Expired, leftover, at-risk, and ownership states | F1 |
| Structural | Facade | `HayPaComerFacade` exposes the system without subsystem details | F2 |
| Structural | Flyweight | Shared food metadata; quantity and dates stay per item | F1 |
| Structural | Proxy | Offline hardware cache and private food protection | F7 |
| Behavioral | Chain of Responsibility | Event validation chain (range, timestamp, mode, stability, duplicates) | F3 |
| Behavioral | Command | Auditable inventory commands | F2 |
| Behavioral | Interpreter | Quantities and units | F5 |
| Behavioral | Iterator | Traversal of the fridge tree | F1 |
| Behavioral | Mediator | Guided cooking session between session, scale, and timers | F5 |
| Behavioral | Memento | Undo and snapshots of inventory state | F2 |
| Behavioral | Observer | SSE panel, notifications, and analytics subscribers | F6 |
| Behavioral | State | Cooking session and cold-chain states with resume | F5 |
| Behavioral | Strategy | Quantity evaluation (enough, reduce, substitute, missing), channels, weekly plan | F5 |
| Behavioral | Template Method | Report generation | F7 |
| Behavioral | Visitor | Analytics over the composite tree | F7 |

Architecture and resilience bonus: Clean Architecture, Repository, DTO, Dependency Injection, MVC, and Circuit Breaker.

## Justification format

Every pattern row is backed by:

1. A real flow where the pattern removes duplication or coupling.
2. A dedicated test that fails if the pattern is removed or misused.
3. A short justification kept in this table.
