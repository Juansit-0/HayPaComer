# Roadmap status

Status tracker for the ~79 steps defined in `PLAN.md` section 9. `PLAN.md` remains the source of truth.

## Phases

| Phase | Scope | Steps | Status |
|---|---|---|---|
| F0 | Foundation | 5 | in progress |
| F0.5 | Brand | 5 + close | pending |
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
- [ ] 4. build and test pipeline + dependabot
- [ ] 5. dependency enforcer + archunit boundary tests

## Notes

- Before the public repository exists, steps are local commits on `main`.
- After repository creation: `feat/*` branch -> PR -> green CI -> review approval -> squash merge.
- JaCoCo coverage checks (>= 80% in `domain` and `application`) are added with the first domain code in F1.
