# Candidate Download — Postgres-backed Version Resolution

The Broker currently resolves candidate-download requests against a MongoDB
`versions` collection in which the version string and distribution are glued
together (e.g. `17.0.2-tem`). Ownership of version metadata is moving to
`sdkman-state`, which persists a normalised schema in PostgreSQL with
`version` and `distribution` as separate columns. The Broker must serve
downloads from that Postgres table while the client-facing contract remains
byte-identical.

*Reference: migration-from-mongo-to-postgres initiative (tracked in sdkman-state).*

## Behaviour

From the CLI client's perspective nothing changes. A client requests a
candidate binary by composing `GET /download/{candidate}/{version}/{platform}`
where `{version}` still carries the historic distribution short-code suffix
for Java (e.g. `17.0.2-tem`). The Broker responds with a `302 Found` whose
`Location` header points at the binary, accompanied by any available
checksum headers and an archive-type header. On a miss the Broker returns
`404 Not Found`; on an unknown platform code it returns `400 Bad Request`.
Every successful redirect continues to produce exactly one row in the
download audit log as a side effect (its row shape is described in the
Business Rules, not the API contract — the audit log is internal storage).

The only observable difference is the data source. After cutover the Broker
reads version records from the Postgres `versions` table owned by
`sdkman-state` instead of the MongoDB `versions` collection. A single
feature toggle (`VENDORS_REPOSITORY`, values `mongo` | `postgres`, default
`mongo`) selects the backend at startup so deployments can cut over and roll
back without code changes.

## API Contract

The HTTP contract below is unchanged by this migration. It is restated here
for completeness so the post-migration behaviour can be verified end-to-end
without cross-referencing the legacy spec. Internal storage shapes (e.g. the
audit row layout) are not part of this contract — they appear under Business
Rules where they are explicitly evolving.

### Request

```
GET /download/{candidate}/{version}/{platform}
```

| Parameter   | In   | Required | Description                                                                                              |
|-------------|------|----------|----------------------------------------------------------------------------------------------------------|
| `candidate` | path | yes      | Candidate identifier (e.g. `java`, `groovy`, `kotlin`).                                                  |
| `version`   | path | yes      | Version token. For Java requests may carry a distribution short-code suffix (e.g. `17.0.2-tem`).         |
| `platform`  | path | yes      | Platform code (`linuxx64`, `linuxarm64`, `darwinx64`, `darwinarm64`, `windowsx64`, `linuxx32`, `exotic`). |

Optional request headers consumed for audit:

| Header       | Description                                 |
|--------------|---------------------------------------------|
| `X-Real-IP`  | Client IP address; consumed by the internal audit side effect. |
| `User-Agent` | Client user agent; consumed by the internal audit side effect. |

### Response

| Status              | Body  | When                                                                                                                          |
|---------------------|-------|-------------------------------------------------------------------------------------------------------------------------------|
| `302 Found`         | empty | Version resolved — `Location` and related headers set. (Audit side effect occurs internally; see Business Rules 10–11.)        |
| `400 Bad Request`   | empty | `platform` is not a recognised platform code.                                                                                 |
| `404 Not Found`     | empty | No matching row for the candidate / version / distribution / platform combination and no UNIVERSAL fallback applies.          |
| `500 Internal Server Error` | empty | Repository failure while reading from the configured backend.                                                          |

### Response Headers (on `302`)

| Header                            | Always | Description                                                                      |
|-----------------------------------|--------|----------------------------------------------------------------------------------|
| `Location`                        | yes    | Binary download URL taken verbatim from the resolved record.                     |
| `X-Sdkman-ArchiveType`            | yes    | Archive type derived from the `Location` URL (`zip`, `tar.gz`, `tar.bz2`, `xz`). |
| `X-Sdkman-Checksum-MD5`           | no     | MD5 checksum, if present on the record.                                          |
| `X-Sdkman-Checksum-SHA-256`       | no     | SHA-256 checksum, if present on the record.                                      |
| `X-Sdkman-Checksum-SHA-512`       | no     | SHA-512 checksum, if present on the record.                                      |

## Business Rules

1. **Distribution is a Java-only concept.** A distribution (e.g. `TEMURIN`,
   `CORRETTO`, `ZULU`) may only be associated with a `java` version. For
   every non-Java record the distribution is true SQL `NULL` — never a
   sentinel value such as `'NA'` or `'NONE'`. The Broker relies on this as a
   data invariant supplied by `sdkman-state` and does not defend against
   sentinel values appearing in the column.
2. **Java versions carry a distribution short-code suffix in the URL.** For
   `candidate = java`, the incoming `{version}` token is of the form
   `{version}-{short-code}` (e.g. `17.0.2-tem`). The Broker strips the suffix
   before lookup and maps the short code to the distribution used for
   matching. If the suffix is not a recognised distribution short code the
   token is used verbatim, which will almost always result in a `404`.
3. **Non-Java versions are never suffix-stripped.** For any candidate other
   than `java`, the `{version}` token is used as-is, even if it happens to
   contain hyphens (e.g. `3.0.0-rc-1`). Distribution is always absent for the
   lookup.
4. **Exact platform match wins.** The Broker first looks for a record whose
   `platform` equals the normalised identifier for the requested `{platform}`
   code and, for Java, whose `distribution` equals the distribution that the
   parsed short code resolves to.
5. **UNIVERSAL fallback.** If the exact platform match misses, the Broker
   retries the lookup with `platform = UNIVERSAL` for the same candidate,
   version, and distribution. If the UNIVERSAL row exists it is served; the
   audit row records `candidate_platform = UNIVERSAL` while `client_platform`
   still reflects the requested platform.
6. **UNIVERSAL fallback preserves distribution.** A Java request that falls
   back to UNIVERSAL still requires a UNIVERSAL row for the same
   distribution. It does not match a UNIVERSAL row belonging to a different
   distribution, nor a UNIVERSAL row with no distribution.
7. **Unknown platform is a client error.** A `{platform}` that does not map
   to a known platform code returns `400` and does not perform any lookup or
   audit side-effect.
8. **Checksum headers reflect available algorithms only.** The Broker emits
   one `X-Sdkman-Checksum-*` header per non-null checksum on the record, and
   only for the algorithms supported by the Postgres schema: MD5, SHA-256,
   SHA-512. Any other algorithms that existed in the MongoDB era are no
   longer surfaced.
9. **Archive type is inferred from the URL.** The `X-Sdkman-ArchiveType`
   header value is determined by the extension of the `Location` URL, not by
   any database field. `.zip` → `zip`, `.tar.gz`/`.tgz` → `tar.gz`,
   `.tar.bz2`/`.bz2` → `tar.bz2`, `.xz` → `xz`. Unknown extensions default to
   `zip`.
10. **Audit is best-effort.** An audit write failure is logged but never
    prevents the `302` response to the client. A 4xx or 5xx response never
    produces an audit row.
11. **Audit uses canonical values.** The `version` field is the
    suffix-stripped version (e.g. `17.0.2`) and the `distribution` field is
    the full distribution enum name (e.g. `TEMURIN`), matching the
    normalised storage in the new `versions` table.
12. **Backend selection is invisible to the client.** The same URL, headers,
    and status codes are produced whether the request is served by the
    MongoDB or Postgres backend. Any discrepancy between backends is a bug.

## Examples

```gherkin
Feature: Candidate Download — Postgres-backed Version Resolution

  Background:
    Given the Broker is configured with VENDORS_REPOSITORY=postgres

  Scenario: Java platform-specific download with distribution suffix
    Given a version record exists with
      | candidate    | java       |
      | version      | 17.0.2     |
      | distribution | TEMURIN    |
      | platform     | MAC_ARM64  |
      | url          | https://example.com/temurin-17.0.2-aarch64-mac.tar.gz |
      | sha_256_sum  | abc123     |
    When the client requests "GET /download/java/17.0.2-tem/darwinarm64"
    Then the response status is 302
      And the "Location" header is "https://example.com/temurin-17.0.2-aarch64-mac.tar.gz"
      And the "X-Sdkman-ArchiveType" header is "tar.gz"
      And the "X-Sdkman-Checksum-SHA-256" header is "abc123"
      And an audit row is written with version "17.0.2", distribution "TEMURIN", candidate_platform "MAC_ARM64"

  Scenario: Non-Java UNIVERSAL fallback
    Given a version record exists with
      | candidate    | groovy    |
      | version      | 4.0.0     |
      | distribution | <null>    |
      | platform     | UNIVERSAL |
      | url          | https://example.com/groovy-4.0.0.zip |
      | sha_256_sum  | def456    |
      | md5_sum      | ghi789    |
    When the client requests "GET /download/groovy/4.0.0/linuxx64"
    Then the response status is 302
      And the "Location" header is "https://example.com/groovy-4.0.0.zip"
      And the "X-Sdkman-ArchiveType" header is "zip"
      And the "X-Sdkman-Checksum-SHA-256" header is "def456"
      And the "X-Sdkman-Checksum-MD5" header is "ghi789"
      And an audit row is written with candidate_platform "UNIVERSAL", client_platform "LINUX_X64", distribution null

  Scenario: Java UNIVERSAL fallback preserves distribution
    Given a version record exists with
      | candidate    | java      |
      | version      | 21.0.1    |
      | distribution | TEMURIN   |
      | platform     | UNIVERSAL |
    When the client requests "GET /download/java/21.0.1-tem/linuxx64"
    Then the response status is 302
      And an audit row is written with distribution "TEMURIN", candidate_platform "UNIVERSAL"

  Scenario: Java UNIVERSAL fallback does not leak across distributions
    Given a version record exists with
      | candidate    | java      |
      | version      | 21.0.1    |
      | distribution | TEMURIN   |
      | platform     | UNIVERSAL |
      And no UNIVERSAL record exists for distribution ZULU
    When the client requests "GET /download/java/21.0.1-zulu/linuxx64"
    Then the response status is 404
      And no audit row is written

  Scenario: Hyphenated non-Java version is not suffix-stripped
    Given a version record exists with
      | candidate    | groovy        |
      | version      | 3.0.0-rc-1    |
      | distribution | <null>        |
      | platform     | UNIVERSAL     |
    When the client requests "GET /download/groovy/3.0.0-rc-1/linuxx64"
    Then the response status is 302

  Scenario: Unknown platform code
    When the client requests "GET /download/java/17.0.2-tem/invalidplatform"
    Then the response status is 400
      And no audit row is written

  Scenario: Unknown candidate
    When the client requests "GET /download/nonexistent/1.0.0/linuxx64"
    Then the response status is 404
      And no audit row is written

  Scenario: Audit failure does not fail the download
    Given the audit store is unavailable
      And a matching version record exists
    When the client requests a valid download URL
    Then the response status is 302
```

## Out of Scope

- Writes to the `versions` table. The Broker remains read-only; writes stay
  with `sdkman-state`.
- Tag-based resolution (`version_tags`) and any tag endpoints.
- Data migration from MongoDB to Postgres. Owned by `sdkman-state`.
- Removal of the MongoDB driver, connectivity, or `application` collection.
  Those persist until the toggle is retired and will be addressed in a
  follow-up.
- The `/download/sdkman/{command}/{version}/{platform}` and
  `/download/native/{command}/{version}/{platform}` endpoints. Neither ever
  touched the `versions` store.
- Surfacing checksum algorithms beyond MD5, SHA-256, SHA-512 (e.g. SHA-1).
- Dual-read or shadow comparison between backends during cutover.

## Acceptance Criteria

- [ ] `GET /download/{candidate}/{version}/{platform}` produces identical
      responses (status, headers, body, audit row) under
      `VENDORS_REPOSITORY=postgres` as under `VENDORS_REPOSITORY=mongo` for every
      scenario in Examples.
- [ ] Java requests with a recognised distribution short-code suffix resolve
      to the matching Postgres row with separate `version` and `distribution`
      values.
- [ ] Non-Java requests resolve against Postgres rows where `distribution IS
      NULL`, with no suffix stripping.
- [ ] UNIVERSAL fallback behaves per Business Rules 5 and 6, including the
      distribution-preservation rule for Java.
- [ ] `X-Sdkman-Checksum-*` headers are emitted only for non-null `md5_sum`,
      `sha_256_sum`, `sha_512_sum` columns on the record.
- [ ] Audit rows record the suffix-stripped `version` and the full
      distribution enum name (e.g. `TEMURIN`, never the short code `tem`).
- [ ] `VENDORS_REPOSITORY` defaults to `mongo`; invalid values fail the
      application at startup with a descriptive error.
- [ ] Acceptance specs exist for the Postgres-backed path mirroring the
      existing Mongo-backed specs, and both run green in CI.
- [ ] All quality gates pass (build, lint, tests).
