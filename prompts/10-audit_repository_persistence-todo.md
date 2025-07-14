# TODO List

## Task 0: Fix broken build

- [X] Correct versions in build file to ensure compatibility

**Description**: The previous oneshot introduce exposed core to the build for persistence. This has bumped kotlinx-coroutines-core to a higher version. This bump has a knock-on effect and is causing the build to break.

**Prompt**: I have a Kotlin Ktor project with a dependency version conflict causing this error: `java.lang.NoSuchMethodError: 'void kotlinx.coroutines.internal.LockFreeLinkedListHead.addLast(kotlinx.coroutines.internal.LockFreeLinkedListNode)'`. The issue is that Exposed 0.61.0 is pulling in kotlinx-coroutines 1.10.1, but Ktor 2.3.7 was compiled against kotlinx-coroutines 1.7.1, causing an API incompatibility. Please update my build.gradle.kts file to fix this by downgrading Exposed to version 0.57.0 (which is compatible with kotlinx-coroutines 1.7.x). Keep all other dependencies the same.

Current Exposed dependencies that need to be changed:
- org.jetbrains.exposed:exposed-core:0.61.0
- org.jetbrains.exposed:exposed-dao:0.61.0
- org.jetbrains.exposed:exposed-jdbc:0.61.0
- org.jetbrains.exposed:exposed-kotlin-datetime:0.61.0

## Task 1: Create Reusable Exception Handling Extension Method

- [X] Extract exception handling logic to reusable extension method

**Description**: Both PostgresHealthRepository and PostgresAuditRepository contain similar exception handling logic that maps database exceptions to domain failures. This logic should be extracted to a reusable extension method on Throwable for better code reuse and conciseness.

**Prompt**: Create a reusable extension method on Throwable that can be used by both PostgresHealthRepository and PostgresAuditRepository to map database exceptions to appropriate domain failures. The extension should handle connection failures and query execution failures, and should use Arrow's Option type for safe string operations. Update both repository classes to use this new extension method, removing the duplicated exception handling logic.

**Files affected**:
- `src/main/kotlin/io/sdkman/broker/adapter/secondary/persistence/PostgresHealthRepository.kt`
- `src/main/kotlin/io/sdkman/broker/adapter/secondary/persistence/PostgresAuditRepository.kt`
- New file: `src/main/kotlin/io/sdkman/broker/adapter/secondary/persistence/DatabaseExceptionExtensions.kt` (or similar)

## Task 2: Create Postgres Test Support Helper Class

- [X] Move database read logic in tests to reusable support class

**Description**: PostgresAuditRepositoryIntegrationSpec contains repeated logic for reading a saved record from the database. This logic should be moved to a Postgres test support class for better reusability across integration tests and for removing clutter from the tests.

**Prompt**: Create a PostgresTestSupport class that provides a helper method for reading a saved record from PostgreSQL integration tests.

**Files affected**:
- `src/test/kotlin/io/sdkman/broker/adapter/secondary/persistence/PostgresAuditRepositoryIntegrationSpec.kt`
- New file: `src/test/kotlin/io/sdkman/broker/support/PostgresTestSupport.kt` (or similar)

## Task 3: Create shouldNotBeNullAnd Helper Extension

- [ ] Introduce reusable helper extension for null checks with type safety

**Description**: The integration tests use a pattern of checking if a value is not null and then performing operations on it. This should be extracted to a reusable helper extension like `shouldNotBeNullAnd { ... }` for better type safety and readability.

**Prompt**: Create a reusable Kotest extension function `shouldNotBeNullAnd` that combines null checking with subsequent assertions. The function should provide type safety by smart-casting the non-null value and allowing chained assertions. Update PostgresAuditRepositoryIntegrationSpec to use this new helper extension, replacing the current `shouldNotBeNull()` followed by `let` pattern.

**Files affected**:
- `src/test/kotlin/io/sdkman/broker/adapter/secondary/persistence/PostgresAuditRepositoryIntegrationSpec.kt`
- `src/test/kotlin/io/sdkman/broker/support/EitherMatchers.kt` (or create new helper file)

## Task 4: Improve Error Assertion Helper Methods

- [ ] Create well-named helper methods for error assertions in tests

**Description**: The integration tests contain TODO comments indicating that error assertion logic should be moved to well-named helper methods. Additionally, there's a need for a better way to retrieve exception messages than using toString().

**Prompt**: Create helper methods for asserting specific types of PersistenceFailure in integration tests. These methods should have descriptive names and provide better ways to access exception messages without relying on toString(). Update PostgresAuditRepositoryIntegrationSpec to use these new helper methods for cleaner and more readable error assertions.

**Files affected**:
- `src/test/kotlin/io/sdkman/broker/adapter/secondary/persistence/PostgresAuditRepositoryIntegrationSpec.kt`
- `src/test/kotlin/io/sdkman/broker/support/EitherMatchers.kt` (or create new helper file)

## Task 5: Make Audit ID Optional for Auto-Generation

- [ ] Allow optional ID in Audit entity for database auto-generation

**Description**: The PostgresAuditRepository currently requires a UUID to be provided for the audit ID. This should be made optional to allow for auto-generation of the primary key in the database, providing more flexibility in how audit records are created.

**Prompt**: Modify the Audit domain entity and PostgresAuditRepository to support optional ID generation. When no ID is provided, the database should auto-generate the UUID using the existing `gen_random_uuid()` default. Update the domain model, repository implementation, and corresponding tests to handle both scenarios: explicit ID assignment and auto-generation.

**Files affected**:
- `src/main/kotlin/io/sdkman/broker/domain/model/Audit.kt`
- `src/main/kotlin/io/sdkman/broker/adapter/secondary/persistence/PostgresAuditRepository.kt`
- `src/test/kotlin/io/sdkman/broker/adapter/secondary/persistence/PostgresAuditRepositoryIntegrationSpec.kt`

## Task 6: Remove Unused Exposed Dependencies

- [ ] Review and remove unnecessary Exposed dependencies from build script

**Description**: The build.gradle.kts file contains a TODO comment questioning whether all Exposed dependencies (core, dao, jdbc, kotlin-datetime) are needed. A review should be conducted to identify and remove any unused dependencies.

**Prompt**: Review the current usage of Jetbrains Exposed dependencies in the codebase and determine which ones are actually needed. The project currently includes exposed-core, exposed-dao, exposed-jdbc, and exposed-kotlin-datetime. Analyze the imports and usage patterns to identify which dependencies can be safely removed without breaking functionality. Update build.gradle.kts to include only the necessary Exposed dependencies.

**Files affected**:
- `build.gradle.kts`
- All files using Exposed (for verification of dependencies)

## Execution plan workflow

The following workflow applies when executing this TODO list:
- Execute one task at a time
- Implement the task in **the simplest way possible**
- Run the tests, format and perform static analysis on the code:
  - ./gradlew test
  - ./gradlew ktlintFormat
  - ./gradlew detekt
- Ask me to review the task once you have completed and **wait for me**
- Mark the TODO item as complete as [X]
- Commit the change to Git when I've approved and/or amended the code
- Move on to the next task
