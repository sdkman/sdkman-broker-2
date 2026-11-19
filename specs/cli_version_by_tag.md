# CLI Version by Tag

Every installed bash CLI asks the legacy broker for the current CLI version
before it self-updates. `sdk selfupdate` fetches
`api.sdkman.io/2/broker/version/sdkman/{script|native}/{stable|beta}` and
compares the bare text it gets back with its local version. CLIs from before
March 2023 also fetch the deprecated `/2/broker/download/sdkman/version/{channel}`
for a daily check. Both URLs are baked into CLIs already installed, so something
has to keep answering them with a bare version string for as long as those CLIs
exist.

Phase 3 switches the legacy broker off, and the two CLIs become State candidates:
`sdkcli` (bash, one `UNIVERSAL` row per release) and `sdkrs` (native, one row per
release per native platform), each tagged `stable` and `beta`. The Broker gains a
route that resolves such a tag to its version and answers it as plain text. The
`api.sdkman.io` nginx front translates both legacy URLs onto it.

The Broker is the **anti-corruption layer** between the legacy CLI contract and
State's model. The plain-text shape lives here, not in `sdkman-state`, whose API
stays JSON only. The route exists only for installed bash CLIs and retires with
them.

*Reference: `../../../docs/specs/application-end-game.md` §4.5 and §8.1, and
`../../../docs/specs/application-execution-plan.md` step 4. Grilled on
2026-11-19.*

**Key Properties:**
- `GET /version/{candidate}/{tag}/{platform}` answers the tagged version as a
  bare `text/plain` string.
- It answers only for `sdkcli` and `sdkrs`.
- Every error has an empty body.
- A success is cacheable at the edge; an error is not.
- Legacy URLs and their fallbacks are the nginx front's job, never the Broker's.

## Rules

**Before implementing, you MUST read and internalize:**
- `.claude/rules/kotlin.md`: Arrow `Either`/`Option`, no nullables, expression bodies.
- `.claude/rules/hexagonal-architecture.md`: the plain-text shape lives in the primary adapter, not the domain.
- `.claude/rules/clean-code.md`: intention-revealing names, small functions.
- `.claude/rules/kotest.md`: outside-in acceptance/integration/unit layering.
- `.claude/rules/quality-gates.md`: no suppressing detekt/ktlint/tests to pass the build.

**If this spec conflicts with the rules, THE RULES WIN.**

## Behaviour

A new public route:

- `GET /version/{candidate}/{tag}/{platform}`

`platform` takes the same codes as the download routes (`linuxx64`,
`darwinarm64`, `universal`, …), matched without regard to case.

On success the answer is `200`, `Content-Type: text/plain; charset=UTF-8`, and
a body holding the version exactly as stored, with no trailing newline: `5.24.0`,
or a bash beta's `latest+5dbdea5`.

The front maps the legacy URLs like this:

| Legacy URL (under `/2/broker`) | Broker URL |
|---|---|
| `/version/sdkman/native/stable` | `/version/sdkrs/stable/linuxx64` |
| `/version/sdkman/native/{anything else}` | `/version/sdkrs/beta/linuxx64` |
| `/version/sdkman/{anything else}/stable` | `/version/sdkcli/stable/universal` |
| `/version/sdkman/{anything else}/{anything else}` | `/version/sdkcli/beta/universal` |
| `/download/sdkman/version/stable` | `/version/sdkcli/stable/universal` |
| `/download/sdkman/version/{anything else}` | `/version/sdkcli/beta/universal` |

That mapping is configuration in `sdkman-ansible`, not Broker code. It is listed
here so the route's callers are known.

### Matching the legacy broker

Through that mapping, every legacy URL answers what the legacy broker answers
today (checked against live `api.sdkman.io/2/broker` on 2026-11-19): the same
fallbacks, the same body byte for byte, and an empty body with no `Content-Type`
on an error. The legacy broker has a single native version for every platform;
reading it from `linuxx64` gives the same value, because native tags move
together. Two differences are accepted, neither visible to an installed CLI:

- **`Content-Type` spacing.** The legacy broker sends `text/plain;charset=UTF-8`;
  the Broker sends Ktor's `text/plain; charset=UTF-8`. CLIs read only the body.
- **Caching.** The legacy broker sends no `Cache-Control`, so a tag move shows at
  once. Here it can take up to the cache lifetime (Business Rule 6).

## Business Rules

1. **The tag decides.** The answer is the version of the row that carries the
   tag for that candidate and platform, with no distribution. Whether the row is
   visible makes no difference, as in State's own tag lookup. The platform must
   match exactly: unlike candidate downloads, there is no fallback to
   `universal`. `sdkcli` only ever has `universal` rows, and `sdkrs` has a row on
   each native platform or none, so a fallback could only give a wrong answer.
2. **CLI candidates only.** The route answers for `sdkcli` and `sdkrs` and for
   no other candidate. Any other candidate is `404`, as if it had no such tag.
   New clients that want a tag's version use State's JSON tag lookup.
3. **Any tag.** The route does not restrict the tag. A tag the candidate does not
   carry on that platform is `404`.
4. **Unknown platform is a client error.** A platform code outside the download
   routes' set is `400`.
5. **Errors are empty.** Every answer other than `200` (`400`, `404`, `500`) has
   an empty body and no `Content-Type`. Errors are logged as on the other routes.
   CLIs from before March 2023 treat an empty or HTML answer as "no news" and keep
   their own version, but they would read a JSON error body as a new version and
   nag on every command.
6. **Success is cacheable, errors are not.** A `200` carries
   `Cache-Control: max-age` with the same lifetime as State's public reads (600
   seconds today), configurable. Every error carries `Cache-Control: no-store`, so
   a passing failure is not pinned at the edge.
7. **Not audited.** A version lookup is not a download and writes no audit row.

## Design Decisions

*Resolved in a grilling session on 2026-11-19, so implementation has no open
questions.*

- **The Broker, not State.** State's tag lookup could have gained a plain-text
  answer. That would have needed a marker parameter, a `406` rule and care that
  Cloudflare, which caches on the URL alone, never serves one shape to a caller
  of the other. Here each URL has exactly one shape, State's API stays JSON only,
  and code that exists only for old CLIs sits in the service that retires with
  them.
- **nginx translates legacy URLs.** As for legacy platform ids (end-game §8.2),
  the front maps the old paths and their fallbacks. The route speaks State's
  candidate and tag names, with no `sdkman`, `script` or `native` in it, and the
  deprecated path never meets the Broker's own `/download/{candidate}/…` route.
- **Path, not query.** The route mirrors the download routes' path shape and
  platform codes, so the Broker's public face keeps one vocabulary.
- **The read widens the Postgres exception.** The Broker reads `version_tags`
  joined to `versions`, over the same read-only connection. The role gains
  `SELECT` on `version_tags`, granted out of band like the existing grant on
  `versions`, before this ships.
- **The edge caches it.** Cloudflare caches an extensionless path only under a
  Cache Rule. A rule scoped strictly to `broker.sdkman.io/version/*` makes the
  route cacheable while downloads stay uncached and audited. That rule is an
  operations step.

## Examples

| Request | Status | Body |
|---|---|---|
| `/version/sdkcli/stable/universal` | `200` | `5.24.0` |
| `/version/sdkcli/beta/UNIVERSAL` | `200` | `latest+5dbdea5` |
| `/version/sdkrs/stable/linuxx64` | `200` | `0.8.0` |
| `/version/sdkrs/stable/linuxarm64` | `200` | `0.8.0` |
| `/version/sdkrs/stable/universal` | `404` | empty |
| `/version/sdkcli/stable/linuxarm64` | `404` | empty |
| `/version/sdkcli/nightly/universal` | `404` | empty |
| `/version/gradle/stable/universal` | `404` | empty |
| `/version/sdkcli/stable/bogus` | `400` | empty |

## Out of Scope

- The nginx front's mapping and the Cloudflare Cache Rule: operations in
  `sdkman-ansible` and Cloudflare.
- Download by tag. The hooks scripts still put the concrete version in the
  download URL, because they also write it to the CLI's local version file.
- Changing the bash CLI's selfupdate. A fix there cannot reach CLIs already
  installed.
- Serving java or any other candidate's tags.

## Acceptance Criteria

- [ ] `/version/sdkcli/stable/universal` answers `200`, `text/plain; charset=UTF-8`, and the version carrying `stable` on `UNIVERSAL`, with no trailing newline.
- [ ] A version with a `+` (`latest+5dbdea5`) comes back byte for byte.
- [ ] `/version/sdkrs/beta/linuxx64` answers the `sdkrs` version tagged `beta` on `LINUX_X64`.
- [ ] Platform codes match without regard to case.
- [ ] A platform with no tagged row answers `404`, even when a `universal` row carries the tag: `/version/sdkcli/stable/linuxarm64` is `404`.
- [ ] A tagged row that is not visible still answers `200`.
- [ ] A candidate other than `sdkcli` or `sdkrs` answers `404`, even when it carries the tag.
- [ ] A tag the candidate does not carry on that platform answers `404`.
- [ ] An unknown platform code answers `400`.
- [ ] Every `400`, `404` and `500` has an empty body, no `Content-Type` and `Cache-Control: no-store`.
- [ ] Every `200` carries `Cache-Control: max-age=600` by default, and the lifetime is configurable.
- [ ] A lookup writes no audit row.
- [ ] Download routes are unchanged: their existing acceptance specs pass unmodified, and they carry no `Cache-Control`.
