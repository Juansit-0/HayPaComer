# ADR 0016: JWT authentication with rotating refresh tokens

- Status: accepted
- Date: 2026-09

## Context

Several people share a household, food has owners, and the API serves a web UI and ESP32 devices. The API must be stateless and every request must be attributable to a person or a device.

## Decision

Use Spring Security in `web` with short-lived access JWTs (15 min, claims `sub`, `hid`, `role`, `jti`, HS256 key from `JWT_SECRET`) and opaque refresh tokens (30 days) stored as SHA-256 hashes in PostgreSQL, rotated on every use with reuse detection by token family. Passwords are hashed with BCrypt behind the `PasswordHasher` port. Authorization is household-scoped with roles `OWNER`, `MEMBER`, and `GUEST`; food ownership rules live in `domain`. ESP32 devices authenticate with a revocable per-device API key sent in `X-Device-Key`.

## Consequences

- Stateless API, easy to test with MockMvc.
- Logout and theft response rely on refresh token revocation; access tokens live at most 15 minutes.
- Email verification, password reset, invitations, and login lockout need their own tables and flows.
