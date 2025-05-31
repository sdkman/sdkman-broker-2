---
description: Kotlin testing conventions with Kotest, Testcontainers, and Wiremock for structured test layers
globs:
alwaysApply: false
---
# Kotlin Testing Rules

*Cursor rules file – testing conventions for **sdkman‑broker** and future Kotlin services.*

> **Intent**
> Provide a repeatable recipe for writing **clear, reliable tests** that align with the functional Kotlin style guide.
> The rules emphasize meaningful assertions, readable structure, and just‑enough coverage across **acceptance, integration, unit, and property** test layers.

---

## 1 Test Layers & Purpose

| Layer | Scope | Tooling |
|-------|-------|---------|
| **Acceptance** | Exercise the application **through its public HTTP API** against a *running instance* backed by a real datastore (Testcontainers Mongo/Postgres). Cover **happy path + relevant unhappy paths**. | Ktor test application or embedded server, Testcontainers, Wiremock (for outbound HTTP stubs). |
| **Integration** | Verify **hexagonal boundaries**: repository classes against real datastores, HTTP client wrappers against Wiremock. No web server. | Kotest + Testcontainers (db) + Wiremock (HTTP) |
| **Unit** | Isolate **pure functions / small helpers** with no side effects. Only used when logic is non‑trivial. | Kotest ShouldSpec |
| **Property‑Based** | Optional: apply to pure functions when *logical boundaries* benefit from data generators. | Kotest property testing |

---

## 2 Framework & Dependencies

* **Kotest 5.x** — the single test framework (`ShouldSpec` style everywhere).
* **Wiremock 3** for stubbing outbound HTTP calls.
* **Testcontainers** for MongoDB and (future) auxiliary services.

Gradle (Kotlin DSL) snippet:

```kotlin
dependencies {
    testImplementation("io.kotest:kotest-runner-junit5:_")
    testImplementation("io.kotest:kotest-assertions-core:_")
    testImplementation("io.kotest.extensions:kotest-extensions-testcontainers:_")
    testImplementation("org.testcontainers:mongodb:_")
    testImplementation("org.testcontainers:postgresql:_")
    testImplementation("org.wiremock:wiremock-standalone:_")
}
```

---

## 3 Naming & Organisation

| Convention | Rule |
|------------|------|
| **File names** | End with `*Spec.kt`. |
| **One class per file** | Each `ShouldSpec` resides alone. |
| **Package** | Mirror production code hierarchy (`io.sdkman.broker.download`). |
| **Class name** | `<TypeUnderTest><Facet>Spec`, e.g. `DownloadRouteAcceptanceSpec`, `AuditRepoIntegrationSpec`, `ChecksumUtilSpec`. |
| **Tags** | `@Tag("acceptance")`, `@Tag("integration")` for slow layers; unit tests untagged. |

---

## 4 ShouldSpec Guidelines

Keep tests **flat** with top‑level `should` blocks. Delineate phases with comments:

```kotlin
class DownloadRouteAcceptanceSpec : ShouldSpec({

    val mongo = MongoContainerListener
    val app = testApplication { application { module() } }

    should("redirect to the universal artefact when platform match is absent") {
        // given: versions collection seeded with UNIVERSAL artefact
        seedVersions(mongo, "spark", "2.3.1")

        // when: client requests non-existent MAC_OSX build
        val call = app.client.get("/download/spark/2.3.1/MAC_OSX")

        // then: redirect to UNIVERSAL artefact with checksum header
        call.status shouldBe HttpStatusCode.Found
        call.headers["Location"] shouldContain "spark-2.3.1.tgz"
    }
})
```

---

## 5 Assertions & Clues

* Use Kotest matchers (`shouldBe`, `shouldContain`, etc.).
* Provide **clues** for fast triage:

```kotlin
call.status shouldBe HttpStatusCode.Found withClue {
    "request=/download/$candidate/$version/$platform"
}
```

---

## 6 Testcontainers & Datastore Rules

* **Singleton** containers via Kotest `Listener` objects (`withReuse(true)` where CI allows).
* Acceptance tests spin up **entire app** wired to the container URI.
* Integration tests talk directly to repositories.

```kotlin
object MongoContainerListener : KMongoContainer("mongo:6").withReuse(true), TestListener
```

---

## 7 Wiremock for Outbound Stubs

* Declare a **WireMockExtension** as Kotest listener:

```kotlin
object ServiceMock : WireMockExtension.newInstance()
    .options(WireMockConfiguration.wireMockConfig().dynamicPort())
    .build()
```

* Integration tests for HTTP client wrappers:

```kotlin
class PaymentClientIntegrationSpec : ShouldSpec({
    listener(ServiceMock)

    should("return success response") {
        // given
        ServiceMock.stubFor(
            post("/charge").willReturn(okJson("{\\"status\\":\\"ok\\"}"))
        )

        // when
        val result = client.charge(100)

        // then
        result shouldBeRight PaymentSuccess
    }
})
```

* Acceptance tests: configure the application under test to call the Wiremock URL via environment override.

---

## 8 Property‑Based Testing

* Use sparingly—only when boundary conditions warrant.
* Default iterations: 100; adjust as needed.
* Generators live in `Generators.kt`; reuse across specs.

---

## 9 Gradle Tasks

| Task | Layers |
|------|--------|
| `test` | Unit, property, fast HTTP tests. |
| `integrationTest` | Specs tagged `integration`. |
| `acceptanceTest` | Specs tagged `acceptance`. |

CI pipeline order: `test` → `integrationTest` → `acceptanceTest`.

---

## 10 Linting & Quality

* Detekt/kotlinter rules forbid:
  * `shouldBe true/false`
  * Empty `should` blocks
  * Non‑descriptive test names
* Every `@Ignore` requires a **reason**.

---

### TL;DR

1. **ShouldSpec** everywhere—flat, readable.
2. **Acceptance / Integration / Unit / PBT** layers each serve a distinct purpose.
3. **Wiremock + Testcontainers** simulate external boundaries.
4. **Descriptive assertions with clues** make failures self‑explanatory.

Place this file with your other Cursor rules to steer AI‑generated tests.
