# spec‑sdkman‑broker

*Functional Kotlin rewrite specification — final consolidated version*

> **Purpose** 
> Re‑implement **sdkman‑broker** in **Kotlin + Arrow** on **Ktor**, with **KMongo** adapters (and Postgres‑ready SPI).
> The legacy broker can be found at https://github.com/sdkman/sdkman-broker.
> Every bullet is a strict, self‑contained requirement.

---

## 0 Overview

The broker is a lightweight edge‑service that:

* Resolves download URLs for SDKMAN! binaries, native client, and candidate artefacts.
* Emits **HTTP 302 redirects** with checksum headers.
* Logs each download into an **Audit** store.
* Serves health and version meta endpoints.
* Publishes client‑version metadata for the SDKMAN! CLI.

---

## 1 Domain Model

| Entity | Collection | Fields |
|--------|------------|--------|
| **Version** | `versions` | `candidate`, `version`, `platform`, `url`, `vendor?`, `visible?`, `checksums?` (map) |
| **App** | `application` (single doc) | `stableCliVersion`, `betaCliVersion`, `stableNativeCliVersion`, `betaNativeCliVersion`, plus `alive="OK"` |
| **AuditEntry** | `audit` | `command`, `candidate`, `version`, `host`, `agent`, `platform`, `date` (epoch ms) |

All domain objects are **immutable Kotlin data classes**.

---

## 2 Public HTTP API

### 2‑1 Meta

| Path | Behaviour |
|------|-----------|
| `GET /health/{name?}` | Aggregated or per‑check health JSON. |
| `GET /version` | Service build banner (plain text). |

### 2‑2 Bash & Native client downloads

`GET /download/sdkman/{command}/{version}/{platform}`  
`GET /download/native/{command}/{version}/{platform}` → redirect built from property templates; async audit.

### 2‑3 Candidate download

`GET /download/{candidate}/{version}/{platform?}` → resolver chooses best platform, sets checksum headers, redirects, audits.

### 2‑4 Client version metadata (critical)

| Path | Response |
|------|----------|
| `GET /download/sdkman/version/{channel}` | Plain text bash CLI version. |
| `GET /version/sdkman/{impl}/{channel}` | JSON `{ "version": "x.y.z" }`. |

---

## 3 Error Model

`Either<BrokerError,*>` mapped: `BadRequest→400`, `NotFound→404`, `RepoFailure→500`.

---

## 4 Persistence SPI

```kotlin
interface VersionRepository { fun find(id: VersionId): IO<RepoError, Option<Version>> }
interface AppRepository     { fun current(): IO<RepoError, App> }
interface AuditRepository   { fun save(e: AuditEntry): IO<RepoError, Unit> }
```

Default impl **KMongo**; future **Postgres** impl plug‑in.

---

## 5 Routing (Ktor)

```kotlin
routing {
  get("/health/{name?}") { healthHandler() }
  get("/version")        { versionHandler() }
  route("/download") {
    get("/sdkman/{cmd}/{ver}/{plat}")   { bashBinaryHandler() }
    get("/native/{cmd}/{ver}/{plat}")   { nativeBinaryHandler() }
    get("/sdkman/version/{channel}")    { cliVersionPlainHandler() }
    get("/{cand}/{ver}/{plat?}")        { candidateHandler() }
  }
  get("/version/sdkman/{impl}/{channel}") { cliVersionJsonHandler() }
}
```

---

## 6 Testing (Kotest)

Follow the cursor rules stipulated in testing-rules.mdc

---

## 7 Non‑functional Targets

Start < 200 ms • P95 redirect < 30 ms • Heap ≤ 128 MiB • Throughput ≥ 500 req/s.

---

## 8 Real‑world Mongo documents

*(identical to v4 appendix; included verbatim)*

### 8‑1 application

```json
{ "_id":"56fe8926bab4a07d6edea175","alive":"OK",
  "stableCliVersion":"5.19.0","betaCliVersion":"latest+b8d230b",
  "stableNativeCliVersion":"0.7.4","betaNativeCliVersion":"0.7.4" }
```

### 8‑2 audit

```json
{ "_id":"00014a77-2d21-4dd3-9e0a-a8543a6e2e2d","command":"install",
  "candidate":"grails","version":"2.3.2","host":"130.127.48.186",
  "agent":"curl/7.22.0 ...","platform":"Linux","date":1384031716903 }
```

### 8‑3 versions (sample entries)

```json
{ "candidate":"spark","version":"2.3.1","platform":"UNIVERSAL",
  "url":"https://archive.apache.org/dist/spark/.../spark-2.3.1.tgz" }

{ "candidate":"java","version":"21.0.1-tem","platform":"MAC_ARM64",
  "url":"https://github.com/.../OpenJDK21U-jdk_aarch64_mac_hotspot_21.0.1_12.tar.gz",
  "vendor":"tem","visible":false }

{ "candidate":"kotlin","version":"2.0.0","platform":"UNIVERSAL",
  "url":"https://github.com/JetBrains/kotlin/.../kotlin-compiler-2.0.0.zip",
  "checksums":{"sha256":"e3b0c44298fc1c149af..."} }
```

Repositories must handle optional **`vendor`**, **`visible`**, **`checksums`** keys.

---

*End of consolidated spec*
