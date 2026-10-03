# Roadmap status

Status tracker for the ~92 steps defined in `PLAN.md` section 9. `PLAN.md` remains the source of truth.

## Phases

| Phase | Scope | Steps | Status |
|---|---|---|---|
| F0 | Foundation | 5 | done |
| F0.5 | Brand | 5 + close | done |
| F0.9 | Data model and domain name | 2 | done |
| F1 | Domain and persistence | 9 | in progress |
| F1.5 | Authentication | 6 | pending |
| F2 | Application | 6 | pending |
| F3 | Door and temperature sensors | 8 | pending |
| F4 | HX711 scale | 5 | pending |
| F5 | Quantities, substitutions, and guided cooking | 8 | pending |
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

## Notes

- Before the public repository exists, steps are local commits on `main`.
- After repository creation: `feat/*` branch -> PR -> green CI -> squash merge by the owner (no review approval required since 2026-10-02).
- JaCoCo line coverage check (>= 80%) is active in `domain` since step 14 and is enabled in `application` with its first use case. Spotless (google-java-format) runs `check` on every build; run `mvn spotless:apply` before committing.
