# Step 92: Release v1.0.0

Commit and pull request title: `chore(release): v1.0.0`

## Goal

Close the roadmap with a versioned, documented release that anyone can run and verify.

## Scope

- Every `pom.xml` moves from `0.1.0-SNAPSHOT` to `1.0.0`; the application jar is `web/target/web-1.0.0.jar`.
- `spring-boot-maven-plugin` also runs `build-info`, so `GET /actuator/info` reports `build.version` 1.0.0 and the artifact.
- `CHANGELOG.md` summarizes what each phase delivered; `docs/releases/v1.0.0.md` holds the release notes.
- `docs/demo.md` starts the 1.0.0 jar.

## After the pull request is merged

The tag and the GitHub release are created from the merged `main` with the team account:

```bash
git switch main && git pull
gh release create v1.0.0 --target main --title "HayPaComer 1.0.0" --notes-file docs/releases/v1.0.0.md
```

## Tests (definition of done)

- `ApiContractIntegrationTest`: `/actuator/info` shows the app name, version 1.0.0, and artifact `web`.
- The full clean build passes and produces `web-1.0.0.jar`.
