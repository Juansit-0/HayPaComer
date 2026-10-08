# Contributing to HayPaComer

Thanks for helping build HayPaComer. This guide gets a new teammate from a clean machine to a merged pull request. Read `PLAN.md` (source of truth) and `AGENTS.md` (current state and rules) before the first change.

## 1. Access

1. The repository owner adds your GitHub account as a collaborator (Settings, Collaborators). Accept the email invitation.
2. Work from your own account only. Never share a personal access token; if one was ever pasted in a chat or a file, revoke it in GitHub (Settings, Developer settings, Personal access tokens) and create a new one.
3. Configure your own identity once, so your commits are attributed to you:

```bash
git config --global user.name "Your Name"
git config --global user.email "the-email-on-your-github-account@example.com"
```

## 2. Machine setup

Requirements: Java 25 (Temurin), Maven 3.9+, Git, and Docker Desktop (running) for the integration tests and the local stack.

```bash
git clone https://github.com/Juansit-0/HayPaComer.git
cd HayPaComer
cp .env.example .env
mvn -q verify
```

- Fill `.env` with your own local values. `.env` is git-ignored; never commit secrets, keys, or `firmware/**/secrets.h`.
- `docker compose up -d` starts PostgreSQL and Redis on `127.0.0.1` for running the app locally.
- Integration tests use Testcontainers and are skipped when Docker is not running; CI always runs them.

## 3. Rules that CI and reviewers enforce

- English everywhere: code, identifiers, tests, docs, and commits. No emojis. No comments in code.
- Hexagonal modules: `domain` and `application` never depend on frameworks; ports are interfaces in `application.port`; adapters live in `adapter-*`; Spring only in adapters and `web`.
- One use case per class with a single public method (ArchUnit checks it).
- JaCoCo coverage of at least 80% in `domain` and `application`; tests use JUnit 5.
- Format with Spotless (google-java-format) before every commit: `mvn -q spotless:apply`.
- Relational data in PostgreSQL with Flyway migrations (never edit a merged migration; add the next `V<n>__name.sql`); AI state only in Redis.

## 4. Workflow for each roadmap step

1. Pick the step assigned to you in `docs/ROADMAP.md` and tell the team in the issue or chat, so nobody else starts it.
2. Update `main` and create a branch: `git switch main && git pull && git switch -c feat/<short-name>`.
3. Implement the step with tests. Keep it small: one roadmap step per pull request.
4. Run `mvn -q spotless:apply` and `mvn verify` until it passes.
5. Commit with Conventional Commits and a scope, for example `feat(planning): 7-day weekly plan with rescue-first strategy`.
6. Push and open a pull request against `main` from your account (`git push -u origin <branch>`, then GitHub or `gh pr create`).
7. Wait for the `build` and `firmware` checks to pass. Fix and push again if they fail.
8. Update the docs your step touches: `docs/ROADMAP.md` (check the step), `AGENTS.md` (state and next action), and `docs/api.md`, `docs/database.md`, `docs/patterns.md`, or `docs/event-protocol.md` when relevant.
9. The owner reviews and squash merges.

When you pair with someone in a session, add a `Co-authored-by: Name <email>` line only for people who actually worked on that change.

## 5. Where things are

| Topic | File |
|---|---|
| Plan and numbered roadmap | `PLAN.md` section 9, `docs/ROADMAP.md` |
| REST endpoints | `docs/api.md` |
| ER diagram and migrations | `docs/database.md`, `adapter-persistence/src/main/resources/db/migration` |
| Design patterns and where they live | `docs/patterns.md` |
| ESP32 envelopes and firmware | `docs/event-protocol.md`, `docs/firmware.md` |
| Example of a full step | any merged `feat(...)` pull request on GitHub |
