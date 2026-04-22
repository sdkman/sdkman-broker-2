# Java Distribution Short-Code Mapping (Derived)

The parent spec `postgres_version_repository.md` requires that, for `candidate
= java`, the Broker strip the `-{short-code}` suffix from the URL `version`
token and translate the short code to the **full distribution enum name** used
in the Postgres `versions.distribution` column and the audit row. The parent
spec gives `tem → TEMURIN` as an example but does not enumerate the full
mapping. This document fixes the mapping the Broker uses.

If `sdkman-state` adds a new Java distribution, add the row here, in the
corresponding Kotlin enum/lookup, and in the relevant tests — in that order.

## Mapping

| Short code | Distribution enum name |
|------------|------------------------|
| `tem`      | `TEMURIN`              |
| `amzn`     | `CORRETTO`             |
| `zulu`     | `ZULU`                 |
| `librca`   | `LIBERICA`             |
| `librcanik`| `LIBERICA_NIK`         |
| `nik`      | `LIBERICA_NIK`         |
| `oracle`   | `ORACLE`               |
| `open`     | `OPEN`                 |
| `graal`    | `GRAALVM_CE`           |
| `graalce`  | `GRAALVM_CE`           |
| `grl`      | `GRAALVM`              |
| `mandrel`  | `MANDREL`              |
| `ms`       | `MICROSOFT`            |
| `sapmchn`  | `SAP_MACHINE`          |
| `sem`      | `SEMERU`               |
| `sem-ce`   | `SEMERU_CE`            |
| `tem-ce`   | `TEMURIN_CE`           |
| `kona`     | `KONA`                 |
| `bsphr`    | `BISHENG`              |
| `dragon`   | `DRAGONWELL`           |
| `jbr`      | `JETBRAINS`            |
| `trv`      | `TRAVA`                |

> **Source of truth.** The list above is a snapshot of the short-codes
> currently in use by `sdkman-state` against the legacy MongoDB `versions`
> collection. Keep it in lock-step with the upstream registry; the test
> Postgres seed data must use these same short-code → enum pairings to be
> representative of production.

## Resolution rules

1. **Java only.** This mapping is consulted only when `candidate == "java"`.
   For every other candidate the URL `version` token is used verbatim and
   distribution is always `NULL` for the lookup (Business Rule 3 of the parent
   spec).
2. **Suffix detection.** The Broker treats the **last** `-`-separated segment
   of the version token as the candidate short code (e.g. for
   `17.0.2.1-2-tem` the short code is `tem`). The remainder is the version
   value used in the lookup.
3. **Unknown short codes.** If the trailing segment does not appear in the
   table above, the version token is used verbatim with no suffix stripping
   and distribution `NULL` (Business Rule 2 of the parent spec). This will
   almost always result in a `404`.
4. **No-suffix Java requests.** If a Java URL has no `-{short-code}` suffix at
   all, the version token is used verbatim with distribution `NULL`. The
   lookup will miss any row whose `distribution` is non-NULL.

## Out of scope

- Surfacing the mapping in any HTTP response — it is purely an internal
  resolver.
- Reverse mapping (enum name → short code) — the Broker never needs to emit
  short codes outwards once the migration completes.
