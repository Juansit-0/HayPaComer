# Step 67: OpenAPI catalog, RFC 7807 errors, and actuator health

Commit and pull request title: `feat(web): openapi catalog, rfc7807 errors, and actuator health`

## Goal

Anyone integrating with HayPaComer (the web UI, the ESP32 team, a teacher reviewing the project) can read the live API contract, every error has the same machine-readable shape, and operations can ask whether the service is alive.

## Scope

- springdoc (`springdoc-openapi-starter-webmvc-ui`, version in the parent pom): `/v3/api-docs` and Swagger UI at `/docs`, only `/api/**` paths, Bearer JWT and `X-Device-Key` security schemes in `OpenApiConfiguration`.
- Actuator: expose only `health` (liveness and readiness probes, no details for anonymous callers) and `info`.
- RFC 7807 everywhere: 401 and 403 from both security chains as `application/problem+json` (with `WWW-Authenticate`), a stable `type` URI per title (`ProblemTypes`), field `errors` for invalid request bodies, and unreadable JSON as 400.
- Security: the documentation and health paths are public (GET only); everything else keeps its rules.

## Tests (definition of done)

- `ApiContractIntegrationTest`: OpenAPI lists the main paths and both schemes, probes answer UP without details, other actuator endpoints stay closed, and 401, validation, unreadable JSON, and domain errors are problem documents.
- `SecurityIntegrationTest` keeps passing.
