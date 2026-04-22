# Postgres `versions` Table Schema (Derived)

The parent spec `postgres_version_repository.md` says the `versions` table is
owned by `sdkman-state` and is read-only from the Broker's perspective, but it
does not define the DDL. This document pins the column shape the Broker reads
against, so the test Flyway migration and the `PostgresVersionRepository`
implementation share a single source of truth. If `sdkman-state` later
publishes a divergent schema, this file (and the test migration) must be
updated to match.

## Table

```sql
CREATE TABLE versions (
    id           UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate    TEXT    NOT NULL,
    version      TEXT    NOT NULL,
    distribution TEXT        NULL,
    platform     TEXT    NOT NULL,
    url          TEXT    NOT NULL,
    visible      BOOLEAN NOT NULL DEFAULT TRUE,
    md5_sum      TEXT        NULL,
    sha_256_sum  TEXT        NULL,
    sha_512_sum  TEXT        NULL
);

CREATE INDEX idx_versions_lookup
    ON versions (candidate, version, distribution, platform);
```

## Column semantics

| Column         | Type     | Null? | Notes                                                                                                  |
|----------------|----------|-------|--------------------------------------------------------------------------------------------------------|
| `id`           | UUID     | no    | Surrogate key. Not consumed by the Broker.                                                             |
| `candidate`    | TEXT     | no    | Candidate identifier (`java`, `groovy`, …). Lower-case.                                                |
| `version`      | TEXT     | no    | Suffix-stripped version (e.g. `17.0.2`, never `17.0.2-tem`).                                           |
| `distribution` | TEXT     | yes   | Full distribution enum name (`TEMURIN`, `CORRETTO`, …). `NULL` for non-Java rows. Never `'NA'`/`'NONE'`. |
| `platform`     | TEXT     | no    | Normalised platform identifier (`LINUX_64`, `MAC_ARM64`, …, `UNIVERSAL`).                              |
| `url`          | TEXT     | no    | Binary download URL emitted verbatim in the `Location` header.                                         |
| `visible`      | BOOLEAN  | no    | Defaults to `TRUE`. The Broker does not currently filter on this column but reads it for parity.       |
| `md5_sum`      | TEXT     | yes   | MD5 checksum hex string.                                                                               |
| `sha_256_sum`  | TEXT     | yes   | SHA-256 checksum hex string.                                                                           |
| `sha_512_sum`  | TEXT     | yes   | SHA-512 checksum hex string.                                                                           |

## Lookup contract

The Broker's only query against this table is a single-row lookup keyed by:

```sql
SELECT * FROM versions
 WHERE candidate    = ?
   AND version      = ?
   AND distribution IS NOT DISTINCT FROM ?   -- NULL-safe equality
   AND platform     = ?
 LIMIT 1;
```

`IS NOT DISTINCT FROM` is required so that `distribution = NULL` matches the
Postgres `NULL` rows for non-Java candidates (Business Rule 1 of the parent
spec). The composite index above covers this query.

## Out of scope

- Writes (INSERT/UPDATE/DELETE) — owned by `sdkman-state`.
- The `version_tags` table and tag resolution.
- Indexes beyond the lookup index (e.g. uniqueness constraints owned by
  `sdkman-state`).
