# HayPaComer database

Presentation mirror of the database of **HayPaComer**, the smart fridge that answers "what can I cook right now with what is actually at home?". Development happens in the monorepo [Juansit-0/HayPaComer](https://github.com/Juansit-0/HayPaComer); this repository is a read-only view updated with `scripts/mirrors.sh` from that monorepo.

## What is here

- `migrations/`: the Flyway migrations that build the schema (relational data in PostgreSQL, AI state in Redis);
- `docs/database.md`: data model and ER diagram;
- `docker-compose.yml` and `.env.example`: PostgreSQL 18 and Redis 8 for local development.

In the monorepo the migrations live in `adapter-persistence/src/main/resources/db/migration` and run automatically at application startup; this mirror only shows them.

## Try it

```bash
cp .env.example .env
docker compose up -d
```

Then point any SQL client at `127.0.0.1:5432` (the port in `.env`).
