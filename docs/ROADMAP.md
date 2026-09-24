# Roadmap status

Status tracker for the ~79 steps defined in `PLAN.md` section 9. `PLAN.md` remains the source of truth.

## Phases

| Phase | Scope | Steps | Status |
|---|---|---|---|
| F0 | Foundation | 5 | done |
| F0.5 | Brand | 5 + close | in progress |
| F1 | Domain and persistence | 7 | pending |
| F2 | Application | 6 | pending |
| F3 | Door and temperature sensors | 8 | pending |
| F4 | HX711 scale | 5 | pending |
| F5 | Quantities, substitutions, and guided cooking | 8 | pending |
| F6 | AI, web, and agent | 19 | pending |
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
- [ ] 8. identity, voice, messaging, and story
- [ ] 9. design system, dtcg tokens, and wcag validation
- [ ] 10. brand guidelines, readme application, and final assets
- [ ] 11. close: rename if needed + public repository + branch protection + history push

## Notes

- Before the public repository exists, steps are local commits on `main`.
- After repository creation: `feat/*` branch -> PR -> green CI -> review approval -> squash merge.
- JaCoCo coverage checks (>= 80% in `domain` and `application`) are added with the first domain code in F1.
