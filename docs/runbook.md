# Production runbook

How HayPaComer runs on Render with the domain haypacomer.dev, and what to do when something goes wrong. The owner holds every account; secrets live only in the Render dashboard and GitHub secrets, never in the repository.

## First deployment (owner, once)

1. **Render account.** Create it at render.com with the GitHub account that owns the repository, and allow Render to read `Juansit-0/HayPaComer`.
2. **Blueprint.** In Render choose New, Blueprint, pick the repository, and accept `render.yaml`. It creates:
   - the web service `haypacomer` (Docker);
   - the database `haypacomer-db` (PostgreSQL);
   - the Key Value `haypacomer-redis`.
   `JWT_SECRET` is generated automatically.
3. **Secrets in the dashboard.** In the web service, Environment, fill `GEMINI_API_KEY` (only to use Gemini; also change `AI_PROVIDER` to `gemini`) and `TELEGRAM_BOT_TOKEN` (only for Telegram alerts). Leave them empty to run offline. The default model is `gemini-flash-latest` and the provider timeout is 60 s (`AI_TIMEOUT`); Google retires older models for new keys, so if the AI suddenly answers with the offline rules, check the log and set `GEMINI_MODEL` to a model your key can use.
4. **First deploy.** Render builds the image and runs Flyway migrations at startup. Open the `onrender.com` address it shows and check:
   - `/actuator/health/readiness` answers `UP`;
   - `/actuator/info` shows version `1.1.0` and the commit.
5. **Smoke checks in GitHub.** In the repository Settings, Secrets and variables, Actions:
   - add the variable `PUBLIC_URL` with the public address (first the `onrender.com` one, later `https://haypacomer.dev`);
   - optionally create an account in the app for checks and add `SMOKE_EMAIL` and `SMOKE_PASSWORD` as secrets.
   From then on, every merge to `main` waits for the new commit to be live and checks it.

## Domain haypacomer.dev

1. In the Render web service, Settings, Custom Domains, add `haypacomer.dev` and `www.haypacomer.dev`.
2. At the registrar, create the records Render shows:
   - for the apex domain, an `A` record to Render's load balancer address (or `ALIAS`/`ANAME` if the registrar supports it);
   - for `www`, a `CNAME` to the `onrender.com` address.
3. Wait for Render to verify the domain and issue the certificate. `.dev` domains only work over HTTPS (HSTS preload), so the site is reachable once the certificate is active.
4. Keep `PUBLIC_BASE_URL` as `https://haypacomer.dev` (links in emails) and update the GitHub variable `PUBLIC_URL`.

## Everyday operation

- **Deploys:** merge to `main` with green CI; Render deploys only after checks pass (`autoDeployTrigger: checksPass`), and the smoke workflow confirms the new commit is live.
- **Health:** Render restarts the container when `/actuator/health/readiness` fails; the image health check uses the same path. `/api/v1/status` lists components running from a saved copy.
- **Logs:** Render dashboard, Logs. Scheduled tasks log a warning and continue when the database is briefly unavailable.
- **AI provider down:** nothing to do; the circuit breaker answers with the offline rules and `/api/v1/status` shows the provider resting.

## Backups and restore

- **Free plans:**
  - the free PostgreSQL has no automatic backups and expires after a limited time, so it is for demos only;
  - the free web service sleeps when idle and wakes up on the next visit.
- **Before real users:** move `haypacomer-db` to a paid plan (daily backups and point in time recovery) and the web service to Starter, by changing `plan` in `render.yaml` and merging.
- **Manual backup at any time:** from a machine with `psql` tools, using the external connection string in the database dashboard:

  ```bash
  pg_dump --format=custom --file haypacomer.dump "$EXTERNAL_DATABASE_URL"
  ```

- **Restore into a new database:**

  ```bash
  pg_restore --clean --no-owner --dbname "$EXTERNAL_DATABASE_URL" haypacomer.dump
  ```

- Redis only holds AI state (memory, conversations, caches, rate limits); losing it never loses stock or safety decisions.

## Rollback

1. In the web service, Events, choose the last good deploy and Rollback. Render serves the previous image in a few seconds.
2. Revert the bad commit on `main` with a pull request so the next deploy does not bring it back.
3. Flyway migrations only move forward. If a release added a migration, the previous image keeps working because migrations add tables and columns without removing them; never edit an applied migration, add a new one instead.

## Demo household in production (optional)

Run the demo seed against the public address after the first deploy. It creates the demo people with generated passwords stored in `.demo-credentials.json`, which Git ignores:

```bash
python3 scripts/demo/seed.py --base https://haypacomer.dev
```

Delete the demo household from the app when it is no longer needed.

## Checklist after each release

- [ ] CI green on `main`, including the `image` job.
- [ ] Smoke workflow green for the release commit.
- [ ] `https://haypacomer.dev` loads in Spanish and English, sign in works, and the session survives closing the tab.
- [ ] `/actuator/info` shows the new version.
- [ ] Tag and GitHub release created.
