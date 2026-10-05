# Design patterns

HayPaComer covers 23/23 GoF patterns. Honesty rule: each pattern is used in a real production flow, has a specific test, and a justified row here. This document is updated in the same PR that implements each pattern.

| Category | Pattern | Role in HayPaComer | First delivery |
|---|---|---|---|
| Creational | Singleton | `FridgeSession` coordinates one physical fridge with a single active state | F6 |
| Creational | Factory Method | `Esp32EventFactory` defines the creation template and `DoorEventFactory`, `TemperatureEventFactory`, and `WeightEventFactory` decide which domain event to build for each envelope type. Test: `Esp32EventAdapterTest` | F3 |
| Creational | Abstract Factory | `HardwareFactory` builds a family of products (`SensorEventDecoder`, `AlertSignal`): `Esp32HardwareFactory` (strict decoding, alerts queued for the ESP32 to pull) and `SimulatedHardwareFactory` (fills event id and time, alerts recorded for the demo); `HardwareFactories` picks the family by device kind. Tests: `HardwareFamiliesTest`, `HardwareFactoriesTest` | F3 |
| Creational | Builder | Builds a suggestion, a weekly plan, or a cooking session step by step | F6 |
| Creational | Prototype | Clonable weekly-plan and recipe templates | F6 |
| Structural | Adapter | `Esp32EventAdapter` adapts the ESP32 JSON envelope (single events or buffered batches) to the `SensorEventDecoder` port and domain `SensorEvent`s; `Esp32Simulator` speaks the same envelope for hardware-free demos. `Hx711ReadingAdapter` adapts raw HX711 counts plus the stored calibration into a `WeightReading`, deciding stability when the device does not report it. Later: AI, OCR, and QR adapters. Test: `Esp32EventAdapterTest` | F3 |
| Structural | Bridge | Abstraction `Interpretation` (`SustainedThreshold`, `StableDelta`) holds an implementor `MeasurementChannel` (`DoorChannel`, `TemperatureChannel`, `WeightChannel`); the same sustained-threshold rule detects a door left open and a cold-chain breach, and `FridgeMonitor` wires them per fridge. Test: `MeasurementBridgeTest` | F3 |
| Structural | Composite | `Fridge` -> `Zone` -> `Tray` -> `FoodItem` behind the sealed `FridgeNode` interface; totals and item counts are computed uniformly at every level. Test: `FridgeTest` | F1 |
| Structural | Decorator | `ExpiredFood`, `AtRiskFood`, `LeftoverFood`, `UnderReviewFood`, and `OwnedFood` wrap a `StockedFood` and stack statuses, rescue priority, edibility, and access; `FreshnessPolicy` applies expiry decorators. Test: `StockedFoodTest` | F1 |
| Structural | Facade | `HayPaComerFacade` hides household access, the fridge composite and iterator, freshness decorators, and rescue ordering behind `setUpFridge`, `fridges`, `inventory`, `rescueFirst`, and `snapshot`; used by `KitchenController` and later by agent tools. Test: `HayPaComerFacadeTest` | F2 |
| Structural | Flyweight | `FoodMetadataCatalog` shares one `FoodMetadata` per food (category, units, conversion factors, allergens); quantity and dates stay per item. Test: `FoodMetadataCatalogTest` | F1 |
| Structural | Proxy | Offline hardware cache and private food protection | F7 |
| Behavioral | Chain of Responsibility | `EventValidator` links `RangeValidator` -> `ClockSkewValidator` -> `StabilityValidator` -> `DuplicateValidator` -> `StaleReadingValidator`; the first link with a verdict (rejected, duplicate, dropped) answers, otherwise the event is accepted. Built by `ValidateSensorEvent`. Test: `ValidationChainTest` | F3 |
| Behavioral | Command | `StockFoodCommand`, `ConsumeFoodCommand`, and `DiscardFoodCommand` implement the sealed `InventoryCommand`; the invoker `ExecuteInventoryCommand` runs each one in a `UnitOfWork`, writes an `AuditEntry`, and replays a repeated command id (Idempotency-Key) without applying it twice. Test: `LiveInventoryTest` | F2 |
| Behavioral | Interpreter | Quantities and units | F5 |
| Behavioral | Iterator | `FridgeTreeIterator` walks any `FridgeNode` depth-first or breadth-first; `FridgeNode` is `Iterable` and exposes `foodItems()` and `trays()` streams used by `Fridge` lookups. Test: `FridgeTreeIteratorTest` | F1 |
| Behavioral | Mediator | Guided cooking session between session, scale, and timers | F5 |
| Behavioral | Memento | `Fridge` (originator) produces an immutable `FridgeMemento` and is rebuilt from it; `InventoryCaretaker` stores household mementos with ownership before every inventory command (multi-level undo) and for manual snapshots the owner can restore. Tests: `FridgeMementoTest`, `UndoAndSnapshotsTest` | F2 |
| Behavioral | Observer | SSE panel, notifications, and analytics subscribers | F6 |
| Behavioral | State | `ColdChain` delegates to `ColdChainState` (`Normal`, `Warming`, `UnderReview`): a breach longer than the grace period moves to under review, recovery alone does not close it, and only a human review after recovery returns to normal. Cooking session states come in F5. Test: `ColdChainTest` | F3 |
| Behavioral | Strategy | Quantity evaluation (enough, reduce, substitute, missing), channels, weekly plan | F5 |
| Behavioral | Template Method | Report generation | F7 |
| Behavioral | Visitor | Analytics over the composite tree | F7 |

Architecture and resilience bonus: Clean Architecture, Repository, DTO, Dependency Injection, MVC, and Circuit Breaker.

## Justification format

Every pattern row is backed by:

1. A real flow where the pattern removes duplication or coupling.
2. A dedicated test that fails if the pattern is removed or misused.
3. A short justification kept in this table.
