# Task guides

Guides for roadmap steps assigned to a teammate. Each guide explains the goal, the code to reuse, the expected scope, the tests, and the definition of done. The design inside each guide is a starting point: if you find a better shape that keeps the rules in `CONTRIBUTING.md`, use it and explain it in the pull request.

| Step | Guide | Owner | Depends on |
|---|---|---|---|
| 61 | [7-day weekly plan with rescue-first strategy](step-61-weekly-plan.md) | Jenifer Urbano (`Jenifrutica`) | F5 merged |
| 62 | [Clonable plan and recipe templates (prototype)](step-62-templates-prototype.md) | Jenifer Urbano (`Jenifrutica`) | 61 |
| 63 | [Plan delta into the market list](step-63-plan-delta-market.md) | Jenifer Urbano (`Jenifrutica`) | 61 |

Steps 56 to 60 and 64 onward stay with the owner unless reassigned here.

## Working in parallel without conflicts

- Flyway: use the next free `V<n>` number when you open the pull request. If `main` takes that number first, rename your file to the next one before merging.
- Spring wiring: add your beans in a new `web/src/main/java/dev/haypacomer/web/planning/PlanningConfiguration.java` instead of editing `KitchenConfiguration`.
- Rebase on `main` (`git pull --rebase origin main`) before asking for the merge.
