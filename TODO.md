# TODO List

## Task 1: Remove Header Assertions from Acceptance Tests

- [x] **Remove header assertions that are not the acceptance test's primary concern**

**Description:** The acceptance tests for version download functionality currently include assertions for HTTP headers like checksums and archive types. These header assertions are not the primary concern of these acceptance tests, which should focus on the main behavior (redirects, status codes). The header assertions should be moved to more focused unit or integration tests.

**Prompt:** Remove the header assertions from the acceptance tests in VersionDownloadAcceptanceSpec.kt. Specifically, remove the assertions for:
- `X-Sdkman-Checksum-*` headers
- `X-Sdkman-ArchiveType` headers

Keep only the core assertions that validate the main acceptance criteria (HTTP status codes, redirect locations). The header functionality should be tested elsewhere in more focused tests.

**Files affected:**
- `src/test/kotlin/io/sdkman/broker/acceptance/VersionDownloadAcceptanceSpec.kt`

---

## Task 2: Extract Document to Version Conversion Logic

- [x] **Convert document-to-version mapping into extension methods**

**Description:** The MongoVersionRepository contains inline document-to-version conversion logic that should be extracted into extension methods for better code organization and reusability. This will make the repository code cleaner and the conversion logic more testable.

**Prompt:** Extract the document-to-version conversion logic in MongoVersionRepository into extension methods. Create extension methods on the Document class:
1. Create a `toVersion()` extension method that converts a MongoDB Document to a Version domain object
2. Replace the inline conversion logic in the `findByQuery` method to use this extension
3. Replace the private `documentToVersion` method with the new extension method
4. Ensure proper handling of optional fields (vendor, visible, checksums) using Arrow's Option types
5. Follow the existing code style and maintain all current functionality

**Files affected:**
- `src/main/kotlin/io/sdkman/broker/adapter/secondary/persistence/MongoVersionRepository.kt`

---

## Task 3: Replace Null Checks with Option and For Comprehension

- [x] **Use Option instead of null checks and implement for comprehension for parameter handling**

**Description:** The download routes currently use null checks for extracting path parameters. This should be replaced with Arrow's Option type and for comprehension to handle the parameters in a more functional way, following the project's functional programming patterns.

**Prompt:** Refactor the parameter extraction in the download routes to use Arrow's Option type instead of null checks. Implement a for comprehension to handle the three path parameters (candidate, version, platform):
1. Convert the parameter extraction to use Option types
2. Use Arrow's for comprehension (either `bindingCatch` or similar) to handle all three parameters together
3. Return appropriate error responses when parameters are missing
4. Maintain the same HTTP status code behavior (400 for bad requests)
5. Follow the existing functional programming patterns used throughout the codebase

**Files affected:**
- `src/main/kotlin/io/sdkman/broker/adapter/primary/rest/DownloadRoutes.kt`

---

## Task 4: Create MongoSupport Helper Object

- [ ] **Create a centralized MongoSupport helper object for test fixtures**

**Description:** Multiple test files contain duplicate MongoDB fixture setup code for inserting test versions. This should be extracted into a centralized `MongoSupport` helper object in the test support package to reduce duplication and improve maintainability.

**Prompt:** Create a new `MongoSupport` helper object in the test support package that centralizes MongoDB fixture management:
1. Create `src/test/kotlin/io/sdkman/broker/support/MongoSupport.kt`
2. Add a `setupVersion(version: Version)` method that handles inserting Version objects into the test database
3. Use the existing `MongoTestListener.database` for database access
4. Handle all Version fields including optional ones (vendor, visible, checksums)
5. Replace all the duplicate fixture setup code in the following test files:
   - `VersionDownloadAcceptanceSpec.kt` (setupVersion function)
   - `MongoVersionRepositoryIntegrationSpec.kt` (multiple inline Document creation blocks)
6. Update all test files to use the new centralized helper
7. Follow the existing code patterns and maintain all current test functionality

**Files affected:**
- `src/test/kotlin/io/sdkman/broker/support/MongoSupport.kt` (new file)
- `src/test/kotlin/io/sdkman/broker/acceptance/VersionDownloadAcceptanceSpec.kt`
- `src/test/kotlin/io/sdkman/broker/adapter/secondary/persistence/MongoVersionRepositoryIntegrationSpec.kt`
