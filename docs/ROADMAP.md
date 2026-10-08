# Roadmap status

Status tracker for the ~92 steps defined in `PLAN.md` section 9. `PLAN.md` remains the source of truth.

## Phases

| Phase | Scope | Steps | Status |
|---|---|---|---|
| F0 | Foundation | 5 | done |
| F0.5 | Brand | 5 + close | done |
| F0.9 | Data model and domain name | 2 | done |
| F1 | Domain and persistence | 9 | done |
| F1.5 | Authentication | 6 | done |
| F2 | Application | 6 | done |
| F3 | Door and temperature sensors | 8 | done |
| F4 | HX711 scale | 5 | done |
| F5 | Quantities, substitutions, and guided cooking | 8 | in progress |
| F6 | AI, web, and agent | 21 | pending |
| F7 | Analytics, robustness, and demo | 16 | pending |

## F0 detail

- [x] 1. maven multi-module structure (8 srp modules) + gitignore + readme
- [x] 2. move proposal and slides to docs/
- [x] 3. roadmap, architecture, patterns, ai, agent, event protocol, and adrs
- [x] 4. build and test pipeline + dependabot
- [x] 5. dependency enforcer + archunit boundary tests

## F0.5 detail

- [x] 6. context, audience, competition, and positioning
- [x] 7. strategy and name evaluation
- [x] 8. identity, voice, messaging, and story
- [x] 9. design system, dtcg tokens, and wcag validation
- [x] 10. brand guidelines, readme application, and final assets
- [x] 11. close: public repository + branch protection + history push (no rename: name confirmed)

## F0.9 detail

- [x] 12. data model, er diagram, api catalog, and adrs 14-17
- [x] 13. rename packages and groupId to dev.haypacomer

## F1 detail

- [x] 14. quantities, units, and food metadata (flyweight)
- [x] 15. fridge-zone-tray-food composite
- [x] 16. iterator to traverse the tree
- [x] 17. recipes, steps, requirements, and members
- [x] 18. food profiles with allergies and diets
- [x] 19. expired, leftover, at-risk, and ownership decorators
- [x] 20. users, households, memberships, and roles
- [x] 21. docker compose with postgresql and redis
- [x] 22. postgresql schema with flyway, repositories, and testcontainers

## F1.5 detail

- [x] 23. register, login, refresh, and logout use cases with ports
- [x] 24. spring security with jwt and rotating refresh tokens
- [x] 25. household-scoped authorization and food ownership checks
- [x] 26. invitations, email verification, and password reset
- [x] 27. esp32 device api keys
- [x] 28. authentication and authorization integration tests

## F2 detail

- [x] 29. haypacomer facade (facade)
- [x] 30. live inventory with permissions
- [x] 31. collaborative market list without duplicates
- [x] 32. auditable inventory commands (command)
- [x] 33. undo and snapshots (memento)
- [x] 34. services and business rules

## F3 detail

- [x] 35. esp32 adapter and simulator (adapter)
- [x] 36. abstract factory for real and simulated hardware
- [x] 37. measurement-interpretation bridge and door alert (bridge)
- [x] 38. event validation chain (chain of responsibility)
- [x] 39. cold chain and under-review state
- [x] 40. rest event intake with device key, validation, and idempotency
- [x] 41. esp32 reed + ds18b20 with json events
- [x] 42. noise, duplicates, and thresholds

## F4 detail

- [x] 43. tare, stable reading, and calibration (hx711 adapter)
- [x] 44. fridge mode with measured stock discount
- [x] 45. cooking mode against recipe requirement
- [x] 46. esp32 hx711 with stable reading
- [x] 47. tare, stability, and calibration

## F5 detail

- [x] 48. enough/reduce/substitute/missing evaluator (strategy)
- [x] 49. quantity and unit interpreter
- [x] 50. substitutions with proportion, limits, and allergies
- [x] 51. cooking session with states and resume (state)
- [x] 52. session, scale, and timer mediator
- [x] 53. timers and guided weighing per step
- [x] 54. automatic missing items to the market list

## Notes

- Before the public repository exists, steps are local commits on `main`.
- After repository creation: `feat/*` branch -> PR -> green CI -> squash merge by the owner (no review approval required since 2026-10-02).
- JaCoCo line coverage check (>= 80%) is active in `domain` since step 14 and in `application` since step 23. Spotless (google-java-format) runs `check` on every build; run `mvn spotless:apply` before committing.
