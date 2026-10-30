# Client Address from Forwarded Headers

The Broker records the client's address on every candidate-download audit row
by reading the `X-Real-IP` request header by hand. Nothing falls back when the
header is missing, so the row's host is empty, and nothing reads the standard
`X-Forwarded-For` header that proxies and CDNs actually set. `sdkman-state` does
this properly: it installs Ktor's `XForwardedHeaders` plugin and reads the
request origin's remote host.

The change matters now because old CLIs are about to reach the Broker through a
proxy. Phase 3 moves pre-5.20.0 CLI candidate downloads off the legacy broker:
the `api.sdkman.io` nginx front proxies them to `broker.sdkman.io`, through
Cloudflare and App Platform. Without forwarded headers, every one of those
downloads would be audited under the front's own address.

*Reference: `../../../docs/specs/07-legacy-candidate-downloads.md`, and
`../../../docs/specs/application-execution-plan.md` step 2d. This change ships
before the front change.*

**Key Properties:**
- The audit host is the client address that the forwarded headers carry, the
  same way `sdkman-state` resolves it.
- With forwarded headers present, the audit host is the **left-most**
  `X-Forwarded-For` entry. Proxies append to the right, so the left-most entry is
  the original client.
- With no forwarded headers, the audit host is the connection's peer address. An
  audit row never records an empty host.
- `X-Real-IP` is no longer read.
- Responses are unchanged: status, `Location`, checksum and archive-type headers,
  and error bodies are byte for byte what they are today.

## Rules

**Before implementing, you MUST read and internalize:**
- `.claude/rules/kotlin.md`: Arrow `Either`/`Option`, no nullables, expression bodies.
- `.claude/rules/hexagonal-architecture.md`: header handling lives in the primary adapter, not the domain.
- `.claude/rules/clean-code.md`: intention-revealing names, small functions.
- `.claude/rules/kotest.md`: outside-in acceptance/integration/unit layering.
- `.claude/rules/quality-gates.md`: no suppressing detekt/ktlint/tests to pass the build.

**If this spec conflicts with the rules, THE RULES WIN.**

## Behaviour

Only the candidate-download route writes audit rows, so only its audit host
changes:

- `GET /download/{candidate}/{version}/{platform}`

The CLI routes (`/download/sdkman/…`, `/download/native/…`) write no audit
today and still write none.

## Business Rules

1. **Forwarded address wins.** When a request carries `X-Forwarded-For`, the
   audit host is its left-most entry, trimmed of whitespace.
2. **Peer address otherwise.** When a request carries no forwarded headers, the
   audit host is the address of the connection that delivered the request.
3. **`X-Real-IP` is ignored.** A request carrying only `X-Real-IP` is audited
   under its peer address, as if the header were absent.
4. **Not a security control.** Clients can set `X-Forwarded-For` themselves. The
   audit host is a statistic, so a spoofed value is accepted. Nothing in the
   Broker authorises on it.
5. **Agent unchanged.** The audit agent is still the `User-Agent` request
   header, as today.

## Examples

| Request headers | Audit host |
|---|---|
| `X-Forwarded-For: 203.0.113.195` | `203.0.113.195` |
| `X-Forwarded-For: 203.0.113.195, 45.55.42.78, 172.68.1.1` | `203.0.113.195` |
| `X-Real-IP: 10.0.0.1` | the peer address |
| none | the peer address |

The second row is the path an old CLI takes once the front proxies it: the CLI's
address, then the `api.sdkman.io` droplet, then the CDN edge.

## Out of Scope

- The `Forwarded` (RFC 7239) header. Nothing in front of the Broker sends it.
- Trusting only known proxies, or counting hops from the right. See Business
  Rule 4.
- Auditing the CLI download routes.
- Changing the audit row's shape, or its other fields.

## Acceptance Criteria

- [ ] A candidate download with `X-Forwarded-For: 203.0.113.195` writes an audit row whose host is `203.0.113.195`.
- [ ] A candidate download with a three-entry `X-Forwarded-For` writes the left-most entry as the host.
- [ ] A candidate download with only `X-Real-IP` writes the peer address, not the `X-Real-IP` value.
- [ ] A candidate download with no forwarded headers writes the peer address, never an empty host.
- [ ] The audit agent is still the request's `User-Agent`.
- [ ] Download responses are unchanged: the existing acceptance specs for status, `Location`, checksum and archive-type headers, and error bodies pass unmodified.
- [ ] The existing audit acceptance specs that send `X-Real-IP` now send `X-Forwarded-For` and expect the same host.
- [ ] No production code reads `X-Real-IP`.
