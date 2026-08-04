# Error Logging & Response Bodies

The Broker currently answers failed requests with a **bodiless** HTTP error and,
for server faults, **discards the underlying cause**. `handleVersionError`
(`DownloadRoutes.kt`) maps every `VersionError` to a status with
`response.status(status)` and nothing else; `PostgresVersionRepository` wraps a
caught `Throwable` in `VersionError.DatabaseError(cause)`, but no layer ever logs
that `cause`. The result is an opaque `500` with `content-length: 0` and no log
line — a real production `versions`-cutover smoke test failed exactly this way
and could not be diagnosed from either the response or the logs.

This spec makes Broker failures **observable**. Two invariants, applied to every
error the Broker can produce:

1. **Every error is logged** — with its cause and stack trace where a `Throwable`
   exists.
2. **Every error response carries a body** — a structured, meaningful message,
   *always*.

*Reference: versions Mongo→Postgres cutover, Phase 1 broker smoke test (see
`../../../docs/specs/05-service-rollout.md`).*

**Key Properties:**
- Every non-2xx response returned by the Broker has a JSON body with a stable
  machine `error` code and a human-readable `message`.
- Every error is logged exactly once, at a severity matching its class: client
  errors (4xx) at `WARN`, server faults (5xx) at `ERROR` with the full stack
  trace of the wrapped cause.
- Server-fault bodies (5xx) never disclose internals (stack traces, SQL, DB role,
  host, connection string); they carry a generic message only. The full stack
  trace is logged server-side, where operators correlate by timestamp and route.
- The success contract is untouched — `302 Found` with `Location`, checksum, and
  archive-type headers is byte-identical to today, and no error body is emitted
  on success.
- HTTP status-code selection is unchanged; this spec adds a body and logging to
  the *existing* status mapping, it does not re-map any error to a new code.

## Rules

**Before implementing, you MUST read and internalize:**
- `.claude/rules/kotlin.md` — Arrow `Either`/`Option`, no nullables, expression bodies.
- `.claude/rules/hexagonal-architecture.md` — logging and HTTP concerns live in the primary adapter, not the domain.
- `.claude/rules/clean-code.md` — intention-revealing names, small functions.
- `.claude/rules/kotest.md` — outside-in acceptance/integration/unit layering.
- `.claude/rules/quality-gates.md` — no suppressing detekt/ktlint/tests to pass the build.

**If this spec conflicts with the rules, THE RULES WIN.**

## Behaviour

From the client's perspective, successful requests are unchanged. A failed
request now returns the same HTTP status as before, but with a JSON body
describing the failure, and the Broker emits a corresponding log line server-side.

The policy is Broker-wide — it applies to every path that can produce a non-2xx
response, not only the candidate-download route:

- `GET /download/{candidate}/{version}/{platform}` (candidate downloads)
- `GET /download/sdkman/{command}/{version}/{platform}` (CLI self-update)
- `GET /download/native/{command}/{version}/{platform}` (native binaries)
- `GET /meta/health`, `GET /meta/release` (meta routes)
- Any **unhandled** exception that escapes a handler (the safety net — see
  Business Rule 7).

## Error Response Contract

### Body shape

Every error response body is a JSON object:

```json
{
  "error": "VERSION_NOT_FOUND",
  "message": "No java version '25.0.4' (TEMURIN) available for platform 'linuxx64'"
}
```

`5xx` responses use the same two-field shape with a deliberately generic message:

```json
{
  "error": "INTERNAL_ERROR",
  "message": "An internal error occurred while processing the request"
}
```

| Field     | Always | Description                                                                        |
|-----------|--------|------------------------------------------------------------------------------------|
| `error`   | yes    | Stable, machine-readable code (SCREAMING_SNAKE_CASE). One per `VersionError` class. |
| `message` | yes    | Human-readable summary. Safe to expose; see Business Rule 4 for 5xx redaction.      |

`Content-Type` is `application/json` (served via the already-installed
`ContentNegotiation`/kotlinx-serialization plugin).

### Status → body/log mapping (candidate-download route)

| `VersionError`            | Status | `error` code        | Log level | Logged detail                                   |
|---------------------------|--------|---------------------|-----------|-------------------------------------------------|
| `InvalidCommand`          | 400    | `INVALID_COMMAND`   | WARN      | offending command token                         |
| `InvalidPlatform`         | 400    | `INVALID_PLATFORM`  | WARN      | offending platform code                         |
| `InvalidVersion`          | 400    | `INVALID_VERSION`   | WARN      | offending version token                         |
| `VersionNotFound`         | 404    | `VERSION_NOT_FOUND` | WARN      | candidate, version, platform                    |
| `DatabaseError`           | 500    | `INTERNAL_ERROR`    | ERROR     | **`cause` with full stack trace**               |

Missing required path parameters (currently `respondBadRequest`) map to `400`
`INVALID_COMMAND` (or a dedicated `MISSING_PARAMETER` code) with a body, logged at
WARN — they must not remain bodiless.

## Business Rules

1. **A body is mandatory on every error.** No Broker error response may have an
   empty body. This includes the parameter-missing `400`s produced before the
   service layer is reached.
2. **Log exactly once, at the boundary.** Each error is logged a single time, in
   the primary adapter (route handler) at the point it is folded into an HTTP
   response — not additionally in the repository/service. This avoids the
   double-logging that arises if every layer logs on the way out. (The existing
   best-effort audit-write log and the health-check log are separate concerns and
   remain.)
3. **Severity matches class.** Client errors (4xx) are expected and log at `WARN`.
   Server faults (5xx) log at `ERROR` and MUST include the wrapped `Throwable` so
   the stack trace reaches the logs.
4. **5xx bodies are opaque; 4xx bodies are specific.** A `4xx` message may name
   the offending input (platform code, version token) — it is the client's own
   data and safe to echo. A `5xx` message MUST be generic; the real cause (SQL
   error text, `permission denied`, role/host/connection details) goes to the log
   only. Nothing about the datastore, schema, or credentials may appear in a
   response body.
5. **Success is untouched.** `2xx`/`3xx` responses gain no body and no error log.
   The `302` redirect, its headers, and the best-effort audit side effect are
   exactly as specified in `postgres_version_repository.md`.
6. **Unhandled exceptions are caught, logged, and bodied.** A `StatusPages`
   (or equivalent) safety net converts any exception that escapes a handler into
   a `500` `INTERNAL_ERROR` body, logged at `ERROR` with the stack trace — so
   "every error is logged and bodied" holds even for faults no `VersionError`
   branch anticipated.
7. **Status codes are stable.** The HTTP status for each error is unchanged from
   current behaviour. This spec adds observability, it does not alter the API's
   status contract (see `postgres_version_repository.md` §Response).

## Design Decisions

*Resolved here so implementation has no open questions. Flagged for review.*

- **JSON body, not plain text.** The Broker already negotiates JSON; a structured
  `{error, message}` is greppable by operators and stable for any future
  programmatic consumer. The CLI ignores error bodies today (it does not pass
  `curl --fail`), so this is additive and breaks nothing.
- **Generic 5xx message, no correlation id (first cut).** Keeps
  datastore/credential details out of a publicly reachable endpoint; the full
  stack trace is logged server-side and operators correlate by timestamp and
  route. A `reference`/correlation id was considered and deferred — it can be
  added later without changing the body's `{error, message}` shape. The
  generic-message / no-internal-leak rule (BR-4) stays regardless.
- **Log at the adapter boundary, once (BR-2).** Centralizes both invariants in one
  choke point and keeps the domain/repository layers free of logging concerns,
  per hexagonal rules.

## Examples

```gherkin
Feature: Observable Broker error responses

  Scenario: Server fault returns a generic body and logs the cause
    Given the configured backend raises a database error on lookup
    When the client requests "GET /download/java/25.0.4-tem/linuxx64"
    Then the response status is 500
      And the response body is JSON with "error" = "INTERNAL_ERROR"
      And the response body "message" is generic and contains no SQL, role, or host detail
      And an ERROR log line is emitted containing the exception stack trace

  Scenario: Unknown platform returns a specific body at WARN
    When the client requests "GET /download/java/25.0.4-tem/nonsense"
    Then the response status is 400
      And the response body is JSON with "error" = "INVALID_PLATFORM"
      And the "message" names the offending platform "nonsense"
      And a WARN log line is emitted

  Scenario: Version miss returns a descriptive 404 body
    Given no matching version record exists
    When the client requests "GET /download/groovy/9.9.9/linuxx64"
    Then the response status is 404
      And the response body is JSON with "error" = "VERSION_NOT_FOUND"
      And the "message" names candidate "groovy", version "9.9.9", platform "linuxx64"
      And a WARN log line is emitted

  Scenario: Missing path parameter is bodied, not empty
    When the client requests a download URL with an empty version segment
    Then the response status is 400
      And the response body is non-empty JSON with an "error" code

  Scenario: Successful download is unchanged
    Given a matching version record exists
    When the client requests a valid download URL
    Then the response status is 302
      And the response body is empty
      And no error log line is emitted
```

## Out of Scope

- Changing any HTTP **status code** — only bodies and logging are added.
- The success-path `302` contract, headers, checksums, and audit side effect
  (owned by `postgres_version_repository.md`).
- The root-cause of the cutover incident itself (a suspected missing `SELECT`
  grant on the `versions` table for the Broker's read-only role). That is an
  **operational/provisioning** fix tracked in the rollout plan's Phase-0
  preflight, not a code change in this spec. This spec ensures that *class* of
  failure is diagnosable next time; it does not grant the role.
- Hardening `/meta/health` to probe the `versions` table instead of `SELECT 1`
  (a related but separate observability gap — track independently).
- Structured/JSON log formatting, log shipping, tracing, or metrics.
- Request/response body logging for successful requests.

## Acceptance Criteria

- [ ] Every non-2xx response from every Broker route returns a JSON body with a
      non-empty `error` code and `message`; no code path emits a bodiless error.
- [ ] Every error is logged exactly once at the adapter boundary, at WARN (4xx) or
      ERROR (5xx).
- [ ] `DatabaseError` (and any unhandled exception) logs the wrapped `Throwable`
      with its stack trace at ERROR, and the response body is generic
      `INTERNAL_ERROR` with no internal detail.
- [ ] `5xx` bodies contain no SQL, DB role, host, connection-string, or
      stack-trace text; `4xx` bodies may name the offending client input.
- [ ] The `302` success contract (status, headers, empty body, audit side effect)
      is unchanged and asserted green.
- [ ] Acceptance specs cover each row of the Status→body/log table, mirroring the
      existing download specs, and run green in CI.
- [ ] All quality gates pass (`./gradlew clean check` — build, detekt, ktlint, tests).
```
