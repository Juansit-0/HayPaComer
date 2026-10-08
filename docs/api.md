# REST API

All business logic, AI, and the agent run in the backend. The web UI and the ESP32 talk to it only through this API and the SSE stream; AI provider keys never leave the server.

- Base path: `/api/v1`. Production host: `https://api.haypacomer.dev` (web UI at `https://haypacomer.dev`).
- Format: JSON; errors as RFC 7807 `application/problem+json`.
- Authentication: `Authorization: Bearer <access JWT>` for people; `X-Device-Key: <key>` for ESP32 devices, accepted only under `/api/v1/device/**` (a separate security chain; device keys never reach user endpoints and JWTs never reach device endpoints).
- Roles are per household: `OWNER`, `MEMBER`, `GUEST`. Food ownership (private, shared, ask first, grants) is checked on every inventory read and write.
- Pagination: `?page=&size=&sort=`; collections return `{ items, page, size, total }`.
- Inventory writes (stock, consume, discard) accept an `Idempotency-Key` UUID header; it becomes the command id, so a retry returns the first outcome with `replayed: true` instead of discounting twice.
- Email links (verification, reset, invitations) carry the token in the URL fragment (`#token=`), so it never reaches server logs; clients post it in the request body.
- OpenAPI at `/v3/api-docs` and Swagger UI at `/swagger-ui` is the contract; this file is the catalog.

Access column: `public` (no token), `user` (any authenticated person), `member` (MEMBER or OWNER of the household), `guest` (any role in the household), `owner`, `device`.

## Authentication

| Method | Path | Access | Purpose |
|---|---|---|---|
| POST | `/auth/register` | public | Create account, send verification email |
| POST | `/auth/login` | public | Email and password -> access JWT + refresh token |
| POST | `/auth/refresh` | public | Rotate refresh token, new access JWT |
| POST | `/auth/logout` | user | Revoke the current refresh token family |
| POST | `/auth/verify-email` | public | Confirm email with token |
| POST | `/auth/forgot-password` | public | Send reset token (same response whether the email exists or not) |
| POST | `/auth/reset-password` | public | Set new password with reset token |

## Me

| Method | Path | Access | Purpose |
|---|---|---|---|
| GET | `/me` | user | Profile and household memberships |
| POST | `/me/email-verification` | user | Resend the verification email |
| PATCH | `/me` | user | Update display name |
| DELETE | `/me` | user | Delete account |
| PUT | `/me/password` | user | Change password (revokes other sessions) |
| GET | `/me/notification-preferences` | user | Channel preferences |
| PUT | `/me/notification-preferences` | user | Update channels and Telegram chat id |

## Households and members

| Method | Path | Access | Purpose |
|---|---|---|---|
| POST | `/households` | user | Create household (caller becomes OWNER) |
| GET | `/households` | user | Households of the caller |
| GET | `/households/{h}` | guest | Household detail |
| PATCH | `/households/{h}` | owner | Rename, currency, timezone |
| DELETE | `/households/{h}` | owner | Delete household |
| GET | `/households/{h}/members` | guest | Members and roles |
| PATCH | `/households/{h}/members/{u}` | owner | Change role |
| DELETE | `/households/{h}/members/{u}` | owner | Remove member (or self leave) |
| POST | `/households/{h}/invitations` | owner | Invite by email with role |
| GET | `/households/{h}/invitations` | owner | Pending invitations |
| DELETE | `/households/{h}/invitations/{id}` | owner | Cancel invitation |
| POST | `/invitations/accept` | user | Join household; token in the body, invitation email must match the account |
| GET | `/households/{h}/profiles` | guest | Food profile of every member (diet, allergies, avoided foods); members without one are omnivores |
| PUT | `/households/{h}/profile` | guest | Replace the caller's own food profile |
| PUT | `/households/{h}/owner` | owner | Transfer ownership to another member |

## Fridges, zones, and trays

| Method | Path | Access | Purpose |
|---|---|---|---|
| POST | `/households/{h}/fridges` | owner | Set up a fridge (`STANDARD` layout by default, or `EMPTY`) |
| GET, PATCH, DELETE | `/fridges/{f}` | guest, owner, owner | Fridge detail, thresholds, delete |
| POST, GET | `/fridges/{f}/zones` | owner, guest | Create, list zones |
| PATCH, DELETE | `/zones/{z}` | owner | Update, delete zone |
| POST, GET | `/zones/{z}/trays` | owner, guest | Create, list trays |
| PATCH, DELETE | `/trays/{t}` | owner | Update, delete tray |
| GET | `/households/{h}/fridges` | guest | Digital twin: every fridge as a tree with grams and item counts |
| GET | `/households/{h}/inventory?rescueFirst=` | guest | Items across fridges in rescue order with freshness statuses |
| GET | `/households/{h}/kitchen` | guest | Kitchen snapshot: items, total grams, at risk, expired |

## Catalog

| Method | Path | Access | Purpose |
|---|---|---|---|
| GET | `/foods?q=` | user | Search food metadata (flyweight catalog) |
| GET | `/foods/{id}` | user | Food detail with allergens |
| POST | `/foods/interpret` | user | `{food, quantity}` text such as "2 tazas", "1,5 kg", "3 huevos + 1/2 kg" into grams with the food conversion factors; 422 when unreadable or the food lacks a density or piece weight |
| GET | `/allergens` | user | Allergen list |
| GET | `/substitution-rules?from=` | user | Allowed substitutions |

## Inventory

| Method | Path | Access | Purpose |
|---|---|---|---|
| GET | `/households/{h}/items` | guest | Inventory, filters by zone, status, owner, expiry |
| POST | `/households/{h}/items` | member (guest for own private items) | Stock weighed food from the catalog: gross grams, tare, tray, expiry, visibility; unknown food answers 422 |
| GET | `/items/{id}` | guest | Item detail (respects ownership) |
| PATCH | `/items/{id}` | member | Move, relabel, change visibility or expiry |
| DELETE | `/items/{id}` | member | Remove item |
| POST | `/households/{h}/items/{id}/consume` | member | Discount grams; ownership checked (private needs a grant, ask-first answers 403) |
| POST | `/households/{h}/items/{id}/discard` | member | Discard as waste |
| PUT | `/households/{h}/items/{id}/visibility` | owner of the item | Shared, ask-first, or private |
| GET | `/households/{h}/items/expiring?days=` | guest | Items expiring soon |
| GET | `/households/{h}/activity?limit=` | guest | Audited inventory commands, newest first |
| POST | `/households/{h}/items/{id}/grants` | owner of the item | Grant access to another member |
| DELETE | `/households/{h}/items/{id}/grants/{u}` | owner of the item | Revoke grant |
| POST | `/households/{h}/inventory/undo` | author of the change or owner | Undo the latest inventory change (multi-level; 409 when nothing is left) |
| GET, POST | `/households/{h}/snapshots` | guest, member | List manual snapshots, take one with an optional reason |
| POST | `/households/{h}/snapshots/{id}/restore` | owner | Restore a manual snapshot and close the undo history |

## Devices and sensor events

| Method | Path | Access | Purpose |
|---|---|---|---|
| POST | `/households/{h}/devices` | owner | Register device; returns API key once |
| GET | `/households/{h}/devices` | guest | Devices with last seen |
| DELETE | `/households/{h}/devices/{d}` | owner | Revoke device key |
| GET | `/device/whoami` | device | Device identity for the ESP32 handshake |
| GET | `/device/commands` | device | Pending alert commands (buzzer, LED) for the device; draining |
| POST | `/device/events` | device | Ingest door, temperature, or weight event (idempotent by event id) |
| GET | `/fridges/{f}/doors` | guest | Door openings |
| GET | `/fridges/{f}/temperatures?from=&to=` | guest | Temperature series |
| GET | `/households/{h}/cold-chain` | guest | Cold-chain state per fridge: phase, since, peak, recovered, last reading |
| POST | `/households/{h}/fridges/{f}/cold-chain/review` | member | Human review after recovery closes the incident (409 while out of range or nothing to review) |

## Scale

| Method | Path | Access | Purpose |
|---|---|---|---|
| POST | `/households/{h}/devices/{d}/scale/tare` | member | Tare with the latest raw sample (409 if the scale has not reported in 10 s) |
| POST | `/households/{h}/devices/{d}/scale/calibrate` | owner | Calibrate with a known weight on the tared scale |
| PUT | `/households/{h}/devices/{d}/scale/mode` | member | Switch to `FRIDGE`, or to `COOKING` with a food and target grams |
| GET | `/households/{h}/devices/{d}/scale/progress` | guest | Cooking progress: measured, target, remaining, percent, SHORT / ON_TARGET (within 3%) / OVER |
| GET | `/households/{h}/devices/{d}/scale/reading` | guest | Latest sample: raw counts and grams once calibrated |
| PUT | `/households/{h}/devices/{d}/scale/item` | member who can use the item | Assign the food item resting on the scale (fridge mode) |
| DELETE | `/households/{h}/devices/{d}/scale/item` | member | Clear the assignment |

## Recipes, cook now, and substitutions

| Method | Path | Access | Purpose |
|---|---|---|---|
| POST, GET | `/households/{h}/recipes` | member, guest | Save a household recipe (requirements with grams or quantity, steps with optional timer and weighing) and list them by name |
| GET, PATCH, DELETE | `/recipes/{id}` | guest, member, member | Recipe detail, update, delete |
| POST | `/households/{h}/recipes/{id}/clone` | member | Copy a household recipe or a global template into the household; the copy shows `clonedFrom` |
| GET | `/recipe-templates` | user | Global recipe templates (5 seeded) |
| POST | `/recipes/{id}/scale` | guest | Rescale portions |
| POST | `/households/{h}/recipes/evaluate` | guest | Inline recipe plus target servings and strategy (STRICT, FLEXIBLE, RESCUE with the substitution rules that every diner can eat; optional `diners` member ids, default the whole household; each requirement takes `grams` or a `quantity` text) evaluated against the usable inventory: ENOUGH, REDUCE, SUBSTITUTE, or MISSING per requirement |
| POST | `/households/{h}/recipes/missing-to-market` | member | Inline recipe plus `targetServings`: every mandatory shortfall against the usable inventory is topped up on the market list (source RECIPE) without adding grams that are already pending; returns shortfall, added, and pending grams per food |
| POST | `/households/{h}/suggestions` | guest | Cook now: minutes, people, equipment -> up to three options with evidence |
| POST | `/households/{h}/rescue` | guest | Rescue mode suggestions |
| POST | `/suggestions/{id}/accept` | member | Accept suggestion (household learning) |
| POST | `/substitutions/propose` | member | Propose substitute for a requirement |
| POST | `/substitutions/{id}/verify` | member | Verify with weighed grams and allergies |
| POST | `/substitutions/{id}/accept` | member | Accept verified substitution |

## Guided cooking

| Method | Path | Access | Purpose |
|---|---|---|---|
| POST | `/households/{h}/cooking-sessions` | member | Start a session (PREPARING) for an inline recipe with steps (instruction, optional `timerSeconds`, optional `weigh` food with grams or quantity) scaled to `targetServings`, optional `scaleId` of a household scale that the session drives; 409 with `activeSessionId` while another session is active |
| GET | `/households/{h}/cooking-sessions/active` | guest | Resume: the active session with its current step, or 204 |
| GET | `/households/{h}/cooking-sessions/{id}` | guest | Current state, step, all steps, and completed steps |
| POST | `/households/{h}/cooking-sessions/{id}/next` | member | PREPARING to step 1, next step, or FINISHED after the last; 409 when paused or over |
| POST | `/households/{h}/cooking-sessions/{id}/pause` | member | Pause the current step |
| POST | `/households/{h}/cooking-sessions/{id}/resume` | member | Resume on the same step |
| POST | `/households/{h}/cooking-sessions/{id}/abandon` | member | Abandon an active session |
| POST | `/households/{h}/cooking-sessions/{id}/steps/{n}/weigh` | member | Guided weighing of the current step: optional `{grams}`, otherwise a fresh calibrated reading of the session scale; returns SHORT / ON_TARGET / OVER with remaining grams (ON_TARGET blinks the scale); the measurement is kept with the completed step; 409 for another step, 400 without grams or scale |
| GET | `/households/{h}/cooking-sessions/{id}/timer` | guest | Current step timer: duration, remaining seconds, paused, done; 204 when the step has none |
| POST | `/households/{h}/cooking-sessions/{id}/finish` | member | Finish and discount used grams (planned) |

## Market list and weekly plan

| Method | Path | Access | Purpose |
|---|---|---|---|
| GET | `/households/{h}/market-list` | guest | Pending items grouped by category plus checked items |
| POST | `/households/{h}/market-list/items` | member | Add a catalog food; a pending duplicate is merged by adding grams |
| PATCH | `/households/{h}/market-list/items/{id}` | member | Change grams |
| POST | `/households/{h}/market-list/items/{id}/check` | member | Mark bought |
| DELETE | `/households/{h}/market-list/items/{id}/check` | member | Unmark (409 if the food is pending again) |
| DELETE | `/households/{h}/market-list/items/{id}` | member | Remove |
| DELETE | `/households/{h}/market-list/checked` | member | Clear bought items |
| POST | `/households/{h}/market-list/from-plan` | member | Add plan delta |
| POST | `/households/{h}/weekly-plans` | member | Generate a 7-day lunch and dinner plan starting today from the saved recipes: food about to expire first, only dishes every diner can eat, no repeat on consecutive days, and `needsShopping` where the fridge falls short; optional `servings` and `diners`; replaces the plan of the same week |
| GET | `/households/{h}/weekly-plans/current` | guest | Plan that covers today with each entry's date; 404 when there is none |
| PATCH | `/households/{h}/plan-entries/{id}` | member | Change the recipe and servings of an entry; `needsShopping` is checked again against the fridge |
| POST | `/households/{h}/weekly-plans/{id}/clone` | member | Copy a plan to another week (`weekStart`, must differ); replaces any plan of that week; 404 for plans of other households |

## Notifications and analytics

| Method | Path | Access | Purpose |
|---|---|---|---|
| GET | `/notifications` | user | Caller notifications |
| PATCH | `/notifications/{id}/read` | user | Mark read |
| GET | `/households/{h}/analytics/summary?month=` | guest | Kg saved, money avoided, waste |
| GET | `/households/{h}/analytics/ranking?month=` | guest | Per-member ranking |
| GET | `/households/{h}/analytics/trend?months=` | guest | Monthly trend |

## AI and agent

| Method | Path | Access | Purpose |
|---|---|---|---|
| POST | `/agent/chat` | guest | Chef chat; streams answer, trace, and confirmation requests over SSE |
| GET | `/agent/conversations` | user | Caller conversations |
| GET | `/agent/conversations/{id}` | user | Conversation messages |
| GET | `/agent/runs/{id}/trace` | user | Visible trace: tools, arguments, results |
| GET | `/agent/confirmations` | user | Pending writes proposed by the agent |
| POST | `/agent/confirmations/{id}/approve` | member | Approve; runs the real use case |
| POST | `/agent/confirmations/{id}/reject` | user | Reject |
| GET | `/households/{h}/agent/memory` | guest | Household memory |
| PATCH | `/households/{h}/agent/memory` | member | Edit memory entries |
| DELETE | `/households/{h}/agent/memory` | owner | Clear memory |
| POST | `/ai/intent` | member | Text -> structured command preview |
| POST | `/ai/label-reader` | member | Photo of label or receipt -> item preview (multipart) |
| POST | `/ai/photo-recipe` | member | Photo -> verifiable recipe preview (multipart) |
| GET | `/households/{h}/ai/audit` | owner | AI latency, valid and rejected responses, fallback usage |

## Realtime and operations

| Method | Path | Access | Purpose |
|---|---|---|---|
| GET | `/households/{h}/stream` | guest | SSE: events, alerts, inventory changes, twin updates |
| GET | `/actuator/health` | public | Health (PostgreSQL, Redis, AI provider) |
| GET | `/v3/api-docs` | public | OpenAPI document |
| GET | `/swagger-ui` | public | API explorer |

Each endpoint ships in the roadmap step of its feature (see `PLAN.md` section 9), with MockMvc tests for success, validation, and authorization failures.
