# AGENTS.md - HayPaComer

> Instructions for any agent (opencode, Claude Code, Cursor, etc.) working on this project.
> Source of truth: **`PLAN.md`** - read it fully before acting.
> The original proposal is in `docs/proposal/` (LaTeX + PDF).

## What this project is

HayPaComer (name confirmed in Phase 0.5) is a smart home fridge: Java application + ESP32 module + kitchen scale, with live inventory, cook-now, cold chain, verifiable substitutions, and an AI agent layer with a multi-agent supervisor. It answers: "what can I cook right now with what is actually at home?".

## Current state (September 2026)

- Master plan v2 approved in `PLAN.md`: ~92 PRs, 8 SRP Maven modules, 23/23 GoF patterns, multi-AI agent, professional brand, expanded demo.
- **19 skills installed globally** (they load in any session): 14 brand (`brand-*`, `target-audience`, `competitor-branding`) + 5 design (`theme-factory`, `design-system`, `effective-ui-design`, `frontend-design`, `ui-ux-kit`) + `impeccable`.
- Local git with step commits; 8-module Maven structure; F0 docs; CI + Dependabot; enforcer + ArchUnit boundary tests. Technical Step 0 complete.
- Brand phase (F0.5) complete in `brand/`: context, audience, competitors, positioning, strategy, naming (name confirmed: **HayPaComer**), identity, voice, messaging, story, launch plan, brand book, and design tokens in `brand/assets/`. Brand close complete: public repository with branch protection.
- **Public repository: https://github.com/Juansit-0/HayPaComer** with `main` protected: PR required, CI check `build` required (strict), admins enforced, 0 review approvals (removed 2026-10-02; the owner merges).

## Where we left off

- Date: 2026-09-24. All Phase 0 (foundation) and Phase 0.5 (brand) work is merged on `main`; CI green; 24 commits of history.
- Hardware for the demo: bill of materials with MercadoLibre Colombia links in `docs/hardware.md`.
- 2026-09-30: plan extended with data model and ER diagram (`docs/database.md`), REST catalog (`docs/api.md`), JWT authentication, PostgreSQL + Flyway for relational data, Redis for all AI state, domain haypacomer.dev (ADRs 0014-0017).
- 2026-10-02: PRs #1 to #11 merged (steps 12-15, Dependabot including JUnit 6.1.3 and ArchUnit 1.5.1). Step 14: `Grams`, `Quantity`, `Unit`, `ConversionFactors`, `FoodMetadata`, `FoodMetadataCatalog` (flyweight); Spotless and JaCoCo active. Step 15: sealed `FridgeNode` with `Fridge`, `Zone`, `Tray`, `FoodItem` (composite).
- Step 16 merged (#12): `FridgeTreeIterator` (depth-first and breadth-first), `FridgeNode` is `Iterable`.
- Step 17 merged (#13): `Recipe`, `RecipeStep`, `RecipeRequirement`, `StepWeighing`, `RecipeSource`, `Member`, `MemberId`; recipes scale to servings and collect allergens.
- Step 18 merged (#14): `Diet`, `FoodProfile`, `ProfileConflict`, `ConflictReason`, `DiningGroup`; checks foods and recipes against allergies, diets, and avoided foods per member.
- Step 19 merged (#15): `StockedFood` with `PlainFood` and decorators `ExpiredFood`, `AtRiskFood`, `LeftoverFood`, `OwnedFood` (`Ownership`, `Visibility`), `FreshnessPolicy`, and `RESCUE_ORDER`.
- Step 20 merged (#16): `User`, `UserId`, `EmailAddress`, `PasswordHash` (identity); `Household` aggregate with `Membership`, `Role`, `Permission`, `AccessDeniedException` (exactly one owner, transfer ownership, membership maps a user to a `MemberId`).
- Step 21 merged (#17): `docker-compose.yml` (PostgreSQL 18, Redis 8 with AOF and password, ports bound to 127.0.0.1) and `.env.example`. Docker is not installed on the development Mac yet.
- Step 22 merged (#18): Spring Boot 4.1.1 BOM imported in the parent; ports `UserRepository`, `HouseholdRepository`, `FoodCatalogRepository`, `FridgeRepository` in `application`; Flyway `V1`/`V2`; `Postgres*Repository` with `JdbcClient`; Testcontainers tests skip locally without Docker and run in CI.
- Step 23 merged (#19): `RegisterUser`, `LogIn` (lockout after 5 failures in 15 min, same error for unknown email), `RefreshSession` (rotation with reuse detection that revokes the family), `LogOut`; ports `PasswordHasher`, `AccessTokenIssuer`, `RefreshTokenStore`, `LoginAttemptLog`; domain `RefreshToken`; JaCoCo active in `application`.
- Step 24 merged (#20): first Spring Boot app (`HayPaComerApplication`); stateless Spring Security with HS256 JWT (`JwtProperties`, `JwtAccessTokenIssuer`, `BCryptPasswordHasher` cost 12); `AuthController` (`/api/v1/auth/register|login|refresh|logout`), `MeController` (`/api/v1/me`), RFC 7807 `ApiExceptionHandler`; `PostgresRefreshTokenStore` and `PostgresLoginAttemptLog`; `GetUserProfile` use case. Requires `JWT_SECRET` (32+ bytes).
- Step 25 merged (#21): household use cases (`CreateHousehold`, `ListHouseholds`, `GetHousehold`, `UpdateHousehold`, `ChangeMemberRole`, `RemoveMember`, `TransferOwnership`) where non-members get 404; `FoodAccessGuard` (shared, private, ask-first, grants, role permissions); `HouseholdController` under `/api/v1/households`; 409 for aggregate invariants; compiler `-parameters` enabled in the parent pom.
- Step 26 merged (#22): `UserToken`/`UserTokenType` and `Invitation` in domain; `RequestEmailVerification`, `VerifyEmail`, `RequestPasswordReset` (silent for unknown emails), `ResetPassword` (revokes all sessions), `InviteMember`, `ListInvitations`, `CancelInvitation`, `AcceptInvitation` (email must match); `EmailSender` port with `LogEmailSender` in `adapter-notifications` (links only when `MAIL_LOG_LINKS=true`); tokens travel in URL fragments.
- Step 27 merged (#23): `Device`, `DeviceId`, `DeviceKind` in domain; `RegisterDevice` (key `hpc_dev_...` shown once, stored as SHA-256), `ListDevices`, `RevokeDevice`, `AuthenticateDevice` (last-seen throttled to 1 min); Flyway `V3__devices`; separate `@Order(1)` security chain for `/api/v1/device/**` with `X-Device-Key`; `GET /api/v1/device/whoami`.
- Step 28 merged (#24): `SecurityIntegrationTest` walks every registered `/api/` endpoint and requires 401 for anonymous callers outside `POST /api/v1/auth/**`; covers forged, expired, `alg: none`, and garbage tokens, household isolation, device-key versus JWT lanes, security headers, stateless responses, and non-enumerating login errors.
- Step 29 merged (#25): `HayPaComerFacade` (setUpFridge, fridges, inventory in the household timezone, rescueFirst, snapshot) over `SetUpFridge` (`FridgeLayout` STANDARD/EMPTY), `ListFridges`, `ViewInventory`; `KitchenController` with `/fridges`, `/inventory`, `/kitchen`; reusable in-memory repositories in `application` test `support`.
- Step 30 merged (#26): `StockFood`, `ConsumeFood` (removes empty items), `DiscardFood`, `ChangeFoodOwnership` (visibility, grant, revoke; item owner only), `SearchFoods`; `InventoryMovement` logged for every change; ports `FoodOwnershipRepository` and `InventoryMovementLog` with PostgreSQL adapters; `ViewInventory` wraps `OwnedFood` and reports `usable` per viewer; Flyway `V4` seeds 24 foods; `InventoryController` and `CatalogController` (`/api/v1/foods?q=`).
- 2026-10-03: Docker Desktop installed on the development Mac (CLI in `~/.docker/bin`); the full suite including Testcontainers runs locally; smoke test with the real jar against `docker compose` passed (842 g to 650 g).
- Step 31 in `feat/application-market-list`: `MarketList` aggregate (`MarketItem`, `MarketSource`; pending duplicates merge grams, grouping by category), `ViewMarketList`, `AddToMarketList`, `UpdateMarketList` (set grams, check, uncheck, remove, clear checked); Flyway `V5`; `PostgresMarketListRepository`; `MarketController`.
- **Next action:** step 32: `feat(application): auditable inventory commands (command)`.
- Workflow: Claude commits, pushes, opens the PR, waits for green CI, and squash merges as the user, with no Claude attribution.

## How to continue (agreed order)

1. **Phases 1-7**: follow the numbered roadmap in `PLAN.md` section 9, one small step at a time, each in a `feat/*` branch with PR, green CI, and squash merge by the owner.

## Project rules (non-negotiable)

- **English everywhere**: code, identifiers, tests, documentation, UI, and commits. No emojis. No comments in code.
- **Conventional Commits in English** with scope; small steps. Once the repository exists: `feat/*` branch -> PR -> green CI -> squash merge (branch protection on `main`). Before the repository: local commits on `main`.
- **8 single-responsibility modules**: `domain`, `application`, `adapter-persistence`, `adapter-sensors`, `adapter-ai`, `adapter-notifications`, `agent`, `web`. No frameworks in `domain`/`application`; boundaries enforced with `maven-enforcer` + ArchUnit.
- Relational data in PostgreSQL; all AI state in Redis. AI runs only in the backend.
- One use case per class with a single public method; segregated ports (ISP); framework annotations only in `web` and adapters.
- **AI never writes directly to the database**: it acts through validated tools with human confirmation and permissions; grams are measured, safety is decided by rules.
- Secrets only in environment variables (`.env`); strict `.gitignore`; never keys in the repository.
- Tests with JUnit 5, JaCoCo coverage >= 80% in `domain`/`application`, Spotless (google-java-format) before every commit.

## Useful commands

```bash
npx skills ls -g
npx skills update -g
mvn -q spotless:apply
mvn -q verify
cp .env.example .env
docker compose up -d
```
