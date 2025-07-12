# TODO List

## Task 1: Refactor HealthService to Use Functional Programming Patterns

- [x] **Replace nullable types and isLeft() checks with Arrow functional programming patterns**

**Description:**
Refactor the HealthService class to eliminate nullable types and replace them with Arrow's `Option` type. Remove all `isLeft()` checks and use proper functional combinators. Implement error accumulation using `zipOrAccumulate` instead of fail-fast validation. This task focuses on making the code more functional and type-safe.

**Prompt:**
You need to refactor the HealthService class located in `src/main/kotlin/io/sdkman/broker/application/service/HealthService.kt` to follow functional programming best practices using Arrow-kt. Replace all nullable types (`String?`, `Instant?`) with Arrow's `Option` type. Remove all `isLeft()` checks and replace them with proper functional combinators like `fold`, `map`, or `flatMap`. Implement error accumulation using `zipOrAccumulate` as described in the Arrow documentation (https://arrow-kt.io/learn/typed-errors/validation/#fail-first-vs-accumulation) instead of the current fail-fast approach. Ensure all methods return proper Either types for error handling and maintain the same business logic while making the code more functional and type-safe.

**Files affected:**
- `src/main/kotlin/io/sdkman/broker/application/service/HealthService.kt`

## Task 2: Refactor PostgresHealthRepository Error Handling and Nullable Types

- [x] **Replace nullable types with Option and exceptions with Either**

**Description:**
Refactor the PostgresHealthRepository to eliminate nullable types and replace them with Arrow's `Option`. Replace exception-based error handling with `Either` types. Add well-named extension methods to ResultSet to hide low-level operations and improve code readability.

**Prompt:**
You need to refactor the PostgresHealthRepository class in `src/main/kotlin/io/sdkman/broker/adapter/secondary/persistence/PostgresHealthRepository.kt` to follow functional programming patterns. Replace all nullable types with Arrow's `Option` type. Change the method signature of `executeHealthCheck` to return `Either` instead of relying on throwing exceptions. Remove the `use` combinator and prefer Arrow's `Option` and `Either` combinators wherever possible. Add a well-named extension method to ResultSet that hides the low-level operations for extracting database values (getString, getTimestamp operations). Ensure all database operations are properly wrapped in Either for error handling and maintain the same functionality while making the code more functional and type-safe.

**Files affected:**
- `src/main/kotlin/io/sdkman/broker/adapter/secondary/persistence/PostgresHealthRepository.kt`

## Task 3: Refactor Test Infrastructure and DataSource Management

- [x] **Move DataSource creation logic to appropriate test listeners and clean up test setup**

**Description:**
Refactor the test infrastructure to move DataSource creation and setup logic from individual test classes to the appropriate PostgresTestListener. Clean up the test dependency injection and remove redundant initialization blocks.

**Prompt:**
You need to refactor the test infrastructure across multiple test files to improve organization and reduce duplication. Move the DataSource creation logic from `TestDependencyInjection.kt` to `PostgresTestListener` and find a cleaner approach that aligns with current project patterns. In `PostgresHealthRepositoryIntegrationSpec.kt`, instantiate the repository normally as a field inside `init` instead of the current approach, move the beforeTest setup into the `PostgresTestListener` where before hooks belong, remove the redundant init block, and find a cleaner way of obtaining a datasource. Ensure all test setup follows consistent patterns and reduces code duplication while maintaining the same test functionality.

**Files affected:**
- `src/test/kotlin/io/sdkman/broker/support/TestDependencyInjection.kt`
- `src/test/kotlin/io/sdkman/broker/adapter/secondary/persistence/PostgresHealthRepositoryIntegrationSpec.kt`

## Task 4: Remove Redundant and Unlikely Test Scenarios

- [ ] **Remove test scenarios that are extremely unlikely to occur**

**Description:**
Remove test methods in PostgresHealthRepositorySpec that test scenarios so unlikely they provide little value. Also remove any other redundant tests identified in the integration specs.

**Prompt:**
You need to clean up the test suite by removing tests that cover extremely unlikely scenarios and provide little value to the test coverage. In `PostgresHealthRepositorySpec.kt`, remove the test scenarios marked as "so unlikely that we will remove the test" - these are testing edge cases that are not practical in real-world usage. In `PostgresHealthRepositoryIntegrationSpec.kt`, remove the test marked as redundant. Ensure that after removing these tests, the remaining test coverage still adequately covers the important business logic and edge cases, while eliminating tests that don't add meaningful value to the test suite.

**Files affected:**
- `src/test/kotlin/io/sdkman/broker/adapter/secondary/persistence/PostgresHealthRepositorySpec.kt`
- `src/test/kotlin/io/sdkman/broker/adapter/secondary/persistence/PostgresHealthRepositoryIntegrationSpec.kt`