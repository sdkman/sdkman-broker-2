# Postgres `versions` Table Schema (Derived)

The parent spec `postgres_version_repository.md` says the `versions` table is
owned by `sdkman-state` and is read-only from the Broker's perspective, but it
does not define the DDL. This document pins the column shape the Broker reads
against, so the test Flyway migration and the `PostgresVersionRepository`
implementation share a single source of truth. The shape recorded here matches
the table that `sdkman-state` currently owns in production. If `sdkman-state`
later publishes a divergent schema, this file (and the test migration) must be
updated to match.

## Table

```sql
CREATE TABLE versions (
    id              SERIAL    PRIMARY KEY,
    candidate       TEXT      NOT NULL,
    version         TEXT      NOT NULL,
    distribution    TEXT          NULL,
    platform        TEXT      NOT NULL,
    visible         BOOLEAN   NOT NULL,
    url             TEXT      NOT NULL,
    md5_sum         TEXT          NULL,
    sha_256_sum     TEXT          NULL,
    sha_512_sum     TEXT          NULL,
    created_at      TIMESTAMP     NULL DEFAULT now(),
    last_updated_at TIMESTAMP     NULL DEFAULT now(),
    UNIQUE (candidate, version, distribution, platform)
);
```

The `UNIQUE` constraint creates the composite index that covers the Broker's
lookup query (see below); no additional index is required.

## Column semantics

| Column            | Type      | Null? | Notes                                                                                                   |
|-------------------|-----------|-------|---------------------------------------------------------------------------------------------------------|
| `id`              | SERIAL    | no    | Surrogate key owned by `sdkman-state`. Not projected or consumed by the Broker.                         |
| `candidate`       | TEXT      | no    | Candidate identifier (`java`, `groovy`, …). Lower-case.                                                 |
| `version`         | TEXT      | no    | Suffix-stripped version (e.g. `17.0.2`, never `17.0.2-tem`).                                            |
| `distribution`    | TEXT      | yes   | Full distribution enum name (`TEMURIN`, `CORRETTO`, …). `NULL` for non-Java rows. Never `'NA'`/`'NONE'`.|
| `platform`        | TEXT      | no    | Normalised platform identifier in the new-style convention (`LINUX_X64`, `MAC_X64`, `MAC_ARM64`, …, `UNIVERSAL`) — shared with the audit table. The legacy Mongo `versions.platform` shape (`LINUX_64`, `MAC_OSX`, …) is not used here. |
| `visible`         | BOOLEAN   | no    | No database-level default; `sdkman-state` sets the value on insert. The Broker reads it for parity.     |
| `url`             | TEXT      | no    | Binary download URL emitted verbatim in the `Location` header.                                          |
| `md5_sum`         | TEXT      | yes   | MD5 checksum hex string.                                                                                |
| `sha_256_sum`     | TEXT      | yes   | SHA-256 checksum hex string.                                                                            |
| `sha_512_sum`     | TEXT      | yes   | SHA-512 checksum hex string.                                                                            |
| `created_at`      | TIMESTAMP | yes   | Row creation timestamp, managed by `sdkman-state`. Not consumed by the Broker.                          |
| `last_updated_at` | TIMESTAMP | yes   | Row mutation timestamp, managed by `sdkman-state`. Not consumed by the Broker.                          |

The Broker's `VersionTable` models only the columns it actually reads
(`candidate`, `version`, `distribution`, `platform`, `visible`, `url`, and the
three checksum columns). `id`, `created_at`, and `last_updated_at` are present
in the table but intentionally absent from the Broker's Exposed mapping.

## Lookup contract

The Broker's only query against this table is a single-row lookup keyed by
`(candidate, version, platform)`, with `distribution` as an optional fourth
component supplied by the caller:

- Java candidates carry a non-`NULL` `distribution` (`TEMURIN`, `CORRETTO`, …).
  The caller supplies a distribution, and the query disambiguates between rows
  that otherwise share the same `(candidate, version, platform)` tuple.
- Non-Java candidates have `distribution IS NULL`. The caller omits the
  distribution, and the query matches the `NULL` rows.

Expressed as a single SQL form using NULL-safe equality:

```sql
SELECT candidate, version, distribution, platform, visible, url,
       md5_sum, sha_256_sum, sha_512_sum
  FROM versions
 WHERE candidate    = ?
   AND version      = ?
   AND platform     = ?
   AND distribution IS NOT DISTINCT FROM ?   -- bound to NULL when caller omits it
 LIMIT 1;
```

`IS NOT DISTINCT FROM NULL` is equivalent to `IS NULL`, so the same query shape
serves both Java and non-Java lookups (Business Rule 1 of the parent spec).
The `UNIQUE (candidate, version, distribution, platform)` constraint
guarantees at most one match and supplies the covering index.

## Out of scope

- Writes (INSERT/UPDATE/DELETE) — owned by `sdkman-state`.
- The `version_tags` table and tag resolution.
- Maintenance of `id`, `created_at`, and `last_updated_at` — owned by
  `sdkman-state`.
